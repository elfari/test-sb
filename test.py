import tkinter as tk
import random

# 창 생성
window = tk.Tk()
window.title("랜덤 이동하는 동그라미")

# 캔버스 생성
canvas = tk.Canvas(window, width=500, height=500, bg="white")
canvas.pack()

# 동그라미 초기 위치와 크기
x, y = 250, 250
radius = 5  # 반지름 5px (지름 10px)

# 동그라미 그리기
circle = canvas.create_oval(x - radius, y - radius, x + radius, y + radius, fill="blue")

# 동그라미 랜덤 이동 함수
def move_circle():
    dx = random.randint(-5, 5)
    dy = random.randint(-5, 5)
    
    coords = canvas.coords(circle)
    x1, y1, x2, y2 = coords

    # 벽에 부딪히면 반대 방향으로 튕기기
    if x1 + dx < 0 or x2 + dx > 500:
        dx = -dx
    if y1 + dy < 0 or y2 + dy > 500:
        dy = -dy

    canvas.move(circle, dx, dy)
    window.after(50, move_circle)  # 50밀리초마다 move_circle 호출

# 이동 시작
move_circle()

# 창 실행
window.mainloop()