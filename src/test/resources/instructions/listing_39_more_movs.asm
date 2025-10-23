bits 16

mov si, bx
mov dh, al

; 8 bit immediate to register moves
mov cx, 12
mov cx, -12

; 16 bit immediate to register moves
mov dx, 3948
mov dx, -3948

; source address calculation moves
;mov al, [bx + si]
;mov ax, [bp + di]
;mov dx, [bp]

; source address calculation plus 8-bit displacement moves
;mov ah, [bx + si + 4]

; source address calculation plus 16-bit displacement moves
;mov al, [bx + si + 4999]

; Dest address calculation moves
;mov [bx + di], cx
;mov [bp + si], cl
;mov [bp], ch
