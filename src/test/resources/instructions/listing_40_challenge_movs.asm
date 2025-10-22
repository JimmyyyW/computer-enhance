
bits 16

; signed displacements
mov ax, [bx + di - 37]
mov [si - 300], cx
mov dx, [bx - 32]

; Explicit sizes
mov [bp + di], byte 7
mov [di + 901], word 347

; direct address
mov bp, [5]
mov bx, [3485]

; memory-to-accumulator
mov ax, [2555]
mov ax, [16]

; accumulator-to-memory
mov [2554], ax
mov [15], al
