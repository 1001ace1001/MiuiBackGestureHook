import unittest
from pathlib import Path

from safe_lsposed_native_deploy import CommandResult, Deployer


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


if __name__ == "__main__":
    unittest.main()
