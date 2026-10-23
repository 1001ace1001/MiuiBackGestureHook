import unittest
from pathlib import Path
from unittest.mock import patch

from safe_lsposed_native_deploy import CommandResult, Deployer, LogSnapshot, parse_args


class RecordingDeployer(Deployer):
    def __init__(self):
        self.commands = []

    def invoke_adb(self, arguments, *, allow_failure=False):
        self.commands.append(list(arguments))
        return CommandResult(0, ("Success",), "Success")


class PackageInstallationTest(unittest.TestCase):
    def test_debug_backup_remains_installable_with_rollback_and_downgrade_flags(self):
        for rollback in (False, True):
            for downgrade in (False, True):
                with self.subTest(rollback=rollback, downgrade=downgrade):
                    deployer = RecordingDeployer()
                    apk = Path("test-only-backup.apk")
                    deployer.install_apk(
                        apk,
                        enable_rollback=rollback,
                        allow_downgrade=downgrade,
                        allow_failure=downgrade,
                    )
                    command, = deployer.commands
                    self.assertEqual(command[0], "install")
                    self.assertEqual(command.count("-t"), 1)
                    self.assertIn("-r", command)
                    self.assertEqual("-d" in command, downgrade)
                    self.assertEqual("--enable-rollback" in command, rollback)
                    if rollback:
                        index = command.index("--enable-rollback")
                        self.assertEqual(command[index + 1], "2")
                    self.assertEqual(command[-1], str(apk))


class HotReloadTest(unittest.TestCase):
    @patch("safe_lsposed_native_deploy.time.sleep")
    def test_strict_install_rejects_deferred_reload_without_reinstalling(self, sleep):
        deployer = RecordingDeployer()
        deployer.get_systemui_pid = lambda: 42
        deployer.read_lsposed_log_delta = lambda _: "Deferred hot reload until recents bar cleanup completes"
        with self.assertRaisesRegex(RuntimeError, "No fresh SystemUI API-102"):
            deployer.wait_api102_hot_reload(42, LogSnapshot())
        self.assertEqual(deployer.commands, [])

    def test_completed_reload_never_needs_another_install(self):
        deployer = RecordingDeployer()
        deployer.get_systemui_pid = lambda: 42
        deployer.read_lsposed_log_delta = lambda _: "Hot reloaded, build=test, process=com.android.systemui"
        self.assertIn("Hot reloaded", deployer.wait_api102_hot_reload(42, LogSnapshot()))
        self.assertEqual(deployer.commands, [])

    def test_strict_deploy_flag_is_explicit(self):
        args = parse_args(["--serial", "test", "--no-hot-reload-retry"])
        self.assertTrue(args.no_hot_reload_retry)
        self.assertFalse(parse_args(["--serial", "test"]).no_hot_reload_retry)


class StatusDeployer(RecordingDeployer):
    def __init__(self, replies):
        super().__init__()
        self.replies = replies
        self.probes = 0

    def invoke_adb(self, arguments, *, allow_failure=False):
        self.commands.append(list(arguments))
        if arguments[:3] == ["shell", "am", "start"]:
            self.probes += 1
            return CommandResult(0, ("Status: ok",), "Status: ok")
        return CommandResult(0, (), "")

    def get_lsposed_log_snapshot(self):
        return self.probes

    def read_lsposed_log_delta(self, snapshot):
        return self.replies[min(self.probes - 1, len(self.replies) - 1)]


class NativeStatusProbeTest(unittest.TestCase):
    @staticmethod
    def reply(ready, native=True):
        return (
            "Published module runtime status reply, "
            f"nativeResponse={str(native).lower()}, statusReady={str(ready).lower()}"
        )

    @patch("safe_lsposed_native_deploy.time.sleep")
    def test_initializing_launcher_can_become_ready_on_a_later_fresh_probe(self, sleep):
        deployer = StatusDeployer([self.reply(False), self.reply(False), self.reply(True)])
        result = deployer.wait_native_ready_status(123)
        self.assertIn("authenticated_native_status=ready", result)
        self.assertEqual(deployer.probes, 3)
        self.assertEqual(sleep.call_count, 41)
        self.assertEqual(deployer.commands[-1][:3], ["shell", "am", "force-stop"])

    @patch("safe_lsposed_native_deploy.time.sleep")
    def test_not_ready_still_fails_after_the_existing_bounded_windows(self, sleep):
        deployer = StatusDeployer([self.reply(False)])
        with self.assertRaisesRegex(RuntimeError, "3 bounded probes") as failure:
            deployer.wait_native_ready_status(123)
        self.assertIn("statusReady=false", str(failure.exception))
        self.assertEqual(deployer.probes, 3)
        self.assertEqual(sleep.call_count, 60)
        self.assertEqual(deployer.commands[-1][:3], ["shell", "am", "force-stop"])

    @patch("safe_lsposed_native_deploy.time.sleep")
    def test_systemui_only_reply_cannot_prove_native_readiness(self, sleep):
        deployer = StatusDeployer([self.reply(True, native=False)])
        with self.assertRaisesRegex(RuntimeError, "3 bounded probes"):
            deployer.wait_native_ready_status(123)
        self.assertEqual(deployer.probes, 3)
        self.assertEqual(sleep.call_count, 60)


if __name__ == "__main__":
    unittest.main()
