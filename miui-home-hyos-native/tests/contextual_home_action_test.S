    .text
    .global TestDartAction
    .type TestDartAction, %function
TestDartAction:
    stp x29, x30, [sp, #-80]!
    stp x19, x20, [sp, #16]
    stp x21, x22, [sp, #32]
    stp x27, x28, [sp, #48]
    mov x29, x0
    mov x28, x1
    mov x27, x2
    mov x22, x3
    mov x19, x4
    ldr x0, [x19]
    mov x8, #0x88
    mov x12, #0x1212
    mov x13, #0x1313
    mov x14, #0x1414
    mov x15, #0x1515
    mov x17, #0x1717
    cmp x0, x0
    bl MiuiHomeHyosDartContextualActionHook
    .global TestDartActionResume
TestDartActionResume:
    stp x0, x1, [x19]
    stp x8, x12, [x19, #16]
    stp x13, x14, [x19, #32]
    stp x15, x17, [x19, #48]
    mrs x0, nzcv
    str x0, [x19, #64]
    ldp x27, x28, [sp, #48]
    ldp x21, x22, [sp, #32]
    ldp x19, x20, [sp, #16]
    ldp x29, x30, [sp], #80
    ret
    .size TestDartAction, .-TestDartAction
    .section .note.GNU-stack,"",%progbits
