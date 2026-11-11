import asyncio
import ctypes
import ctypes.wintypes
import json
import random
import socket
import string
import struct
import threading
import time
import tkinter as tk
from io import BytesIO

import qrcode
import websockets
from PIL import Image, ImageTk

# ---------- SendInput structures ----------

INPUT_MOUSE = 0
INPUT_KEYBOARD = 1

MOUSEEVENTF_MOVE = 0x0001
MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_RIGHTDOWN = 0x0008
MOUSEEVENTF_RIGHTUP = 0x0010

KEYEVENTF_KEYUP = 0x0002
VK_CONTROL = 0x11


class MOUSEINPUT(ctypes.Structure):
    _fields_ = [
        ("dx", ctypes.wintypes.LONG),
        ("dy", ctypes.wintypes.LONG),
        ("mouseData", ctypes.wintypes.DWORD),
        ("dwFlags", ctypes.wintypes.DWORD),
        ("time", ctypes.wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
    ]


class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", ctypes.wintypes.WORD),
        ("wScan", ctypes.wintypes.WORD),
        ("dwFlags", ctypes.wintypes.DWORD),
        ("time", ctypes.wintypes.DWORD),
        ("dwExtraInfo", ctypes.POINTER(ctypes.c_ulong)),
    ]


class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [
        ("uMsg", ctypes.wintypes.DWORD),
        ("wParamL", ctypes.wintypes.WORD),
        ("wParamH", ctypes.wintypes.WORD),
    ]


class INPUT_UNION(ctypes.Union):
    _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]


class INPUT(ctypes.Structure):
    _fields_ = [("type", ctypes.wintypes.DWORD), ("union", INPUT_UNION)]


SendInput = ctypes.windll.user32.SendInput
SendInput.argtypes = [ctypes.c_uint, ctypes.POINTER(INPUT), ctypes.c_int]
SendInput.restype = ctypes.c_uint


def send_mouse_move(dx: int, dy: int):
    inp = INPUT()
    inp.type = INPUT_MOUSE
    inp.union.mi.dx = dx
    inp.union.mi.dy = dy
    inp.union.mi.dwFlags = MOUSEEVENTF_MOVE
    SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def send_mouse_click(button: str = "left"):
    down_flag = MOUSEEVENTF_LEFTDOWN if button == "left" else MOUSEEVENTF_RIGHTDOWN
    up_flag = MOUSEEVENTF_LEFTUP if button == "left" else MOUSEEVENTF_RIGHTUP
    inp_down = INPUT()
    inp_down.type = INPUT_MOUSE
    inp_down.union.mi.dwFlags = down_flag
    inp_up = INPUT()
    inp_up.type = INPUT_MOUSE
    inp_up.union.mi.dwFlags = up_flag
    arr = (INPUT * 2)(inp_down, inp_up)
    SendInput(2, arr, ctypes.sizeof(INPUT))


def send_mouse_down():
    inp = INPUT()
    inp.type = INPUT_MOUSE
    inp.union.mi.dwFlags = MOUSEEVENTF_LEFTDOWN
    SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def send_mouse_up():
    inp = INPUT()
    inp.type = INPUT_MOUSE
    inp.union.mi.dwFlags = MOUSEEVENTF_LEFTUP
    SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def send_key_down(vk: int):
    inp = INPUT()
    inp.type = INPUT_KEYBOARD
    inp.union.ki.wVk = vk
    SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


def send_key_up(vk: int):
    inp = INPUT()
    inp.type = INPUT_KEYBOARD
    inp.union.ki.wVk = vk
    inp.union.ki.dwFlags = KEYEVENTF_KEYUP
    SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))


# ---------- PowerPoint COM helper ----------

class PowerPointController:
    def __init__(self):
        self._app = None
        self._last_state = None

    def _get_app(self):
        import win32com.client
        try:
            if self._app is None:
                self._app = win32com.client.Dispatch("PowerPoint.Application")
            _ = self._app.Visible
            return self._app
        except Exception:
            self._app = None
            try:
                self._app = win32com.client.Dispatch("PowerPoint.Application")
                return self._app
            except Exception:
                self._app = None
                return None

    def _get_show_view(self):
        app = self._get_app()
        if app is None:
            return None
        try:
            if app.SlideShowWindows.Count == 0:
                return None
            return app.SlideShowWindows(1).View
        except Exception:
            return None

    def _get_presentation(self):
        app = self._get_app()
        if app is None:
            return None
        try:
            if app.Presentations.Count == 0:
                return None
            return app.ActivePresentation
        except Exception:
            return None

    def start_show(self):
        pres = self._get_presentation()
        if pres is None:
            return {"type": "error", "message": "No presentation open"}
        try:
            pres.SlideShowSettings.Run()
            return None
        except Exception as e:
            return {"type": "error", "message": str(e)}

    def stop_show(self):
        view = self._get_show_view()
        if view is None:
            return {"type": "error", "message": "No slide show running"}
        try:
            view.Exit()
            return None
        except Exception as e:
            return {"type": "error", "message": str(e)}

    def next_slide(self):
        view = self._get_show_view()
        if view is None:
            return {"type": "error", "message": "No slide show running"}
        try:
            view.Next()
            return None
        except Exception as e:
            return {"type": "error", "message": str(e)}

    def prev_slide(self):
        view = self._get_show_view()
        if view is None:
            return {"type": "error", "message": "No slide show running"}
        try:
            view.Previous()
            return None
        except Exception as e:
            return {"type": "error", "message": str(e)}

    def toggle_blank(self):
        view = self._get_show_view()
        if view is None:
            return {"type": "error", "message": "No slide show running"}
        try:
            if view.State == 3:  # ppSlideShowBlackScreen
                view.State = 1  # ppSlideShowRunning
            else:
                view.State = 3
            return None
        except Exception as e:
            return {"type": "error", "message": str(e)}

    def get_state(self):
        app = self._get_app()
        if app is None:
            return {"type": "error", "message": "PowerPoint is not running"}

        pres = self._get_presentation()
        if pres is None:
            return {"type": "error", "message": "No presentation open"}

        view = self._get_show_view()
        if view is None:
            return {
                "type": "state",
                "slide": 0,
                "total": pres.Slides.Count,
                "showRunning": False,
                "blank": False,
                "notes": "",
            }

        try:
            slide_index = view.CurrentShowPosition
            total = pres.Slides.Count
            is_blank = view.State == 3

            notes = ""
            try:
                slide = pres.Slides(slide_index)
                placeholders = slide.NotesPage.Shapes.Placeholders
                if placeholders.Count >= 2:
                    notes = placeholders(2).TextFrame.TextRange.Text
            except Exception:
                try:
                    slide = pres.Slides(slide_index)
                    for i in range(1, slide.NotesPage.Shapes.Count + 1):
                        shape = slide.NotesPage.Shapes(i)
                        try:
                            if shape.HasTextFrame and shape.TextFrame.HasText:
                                txt = shape.TextFrame.TextRange.Text
                                if txt.strip():
                                    notes = txt
                                    break
                        except Exception:
                            continue
                except Exception:
                    pass

            return {
                "type": "state",
                "slide": slide_index,
                "total": total,
                "showRunning": True,
                "blank": is_blank,
                "notes": notes,
            }
        except Exception as e:
            return {"type": "error", "message": str(e)}


# ---------- Server state ----------

PIN = "".join(random.choices(string.digits, k=6))
PORT = 8765
SENSITIVITY = 1.5

ppt = PowerPointController()
authenticated_clients: set = set()
connected_clients: set = set()
failed_attempts: dict = {}  # ip -> (count, first_fail_time)
LOCKOUT_THRESHOLD = 5
LOCKOUT_SECONDS = 60

last_broadcast_state: dict | None = None


def get_lan_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def is_locked_out(ip: str) -> bool:
    if ip not in failed_attempts:
        return False
    count, first_time = failed_attempts[ip]
    if time.time() - first_time > LOCKOUT_SECONDS:
        del failed_attempts[ip]
        return False
    return count >= LOCKOUT_THRESHOLD


def record_failed_attempt(ip: str):
    now = time.time()
    if ip in failed_attempts:
        count, first_time = failed_attempts[ip]
        if now - first_time > LOCKOUT_SECONDS:
            failed_attempts[ip] = (1, now)
        else:
            failed_attempts[ip] = (count + 1, first_time)
    else:
        failed_attempts[ip] = (1, now)


# ---------- WebSocket handler ----------

async def handler(websocket):
    client_id = id(websocket)
    ip = websocket.remote_address[0] if websocket.remote_address else "unknown"
    connected_clients.add(websocket)
    is_authed = False

    try:
        async for raw in websocket:
            try:
                msg = json.loads(raw)
            except json.JSONDecodeError:
                await websocket.send(json.dumps({"type": "error", "message": "Invalid JSON"}))
                continue

            msg_type = msg.get("type")

            if not is_authed:
                if msg_type != "auth":
                    await websocket.send(json.dumps({"type": "error", "message": "Not authenticated"}))
                    continue

                if is_locked_out(ip):
                    await websocket.send(json.dumps({"type": "auth", "ok": False}))
                    await websocket.send(json.dumps({"type": "error", "message": "Too many attempts. Try again later."}))
                    continue

                if msg.get("pin") == PIN:
                    is_authed = True
                    authenticated_clients.add(websocket)
                    await websocket.send(json.dumps({"type": "auth", "ok": True}))
                    state = ppt.get_state()
                    await websocket.send(json.dumps(state))
                else:
                    record_failed_attempt(ip)
                    await websocket.send(json.dumps({"type": "auth", "ok": False}))
                continue

            if msg_type == "cmd":
                action = msg.get("action")
                result = None
                if action == "next":
                    result = ppt.next_slide()
                elif action == "prev":
                    result = ppt.prev_slide()
                elif action == "start":
                    result = ppt.start_show()
                elif action == "stop":
                    result = ppt.stop_show()
                elif action == "blank":
                    result = ppt.toggle_blank()
                else:
                    result = {"type": "error", "message": f"Unknown action: {action}"}

                if result:
                    await websocket.send(json.dumps(result))
                else:
                    await asyncio.sleep(0.1)
                    state = ppt.get_state()
                    await broadcast_state(state)

            elif msg_type == "move":
                dx = int(msg.get("dx", 0) * SENSITIVITY)
                dy = int(msg.get("dy", 0) * SENSITIVITY)
                send_mouse_move(dx, dy)

            elif msg_type == "click":
                button = msg.get("button", "left")
                send_mouse_click(button)

            elif msg_type == "laser":
                laser_state = msg.get("state")
                if laser_state == "down":
                    send_key_down(VK_CONTROL)
                    send_mouse_down()
                elif laser_state == "up":
                    send_mouse_up()
                    send_key_up(VK_CONTROL)

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        connected_clients.discard(websocket)
        authenticated_clients.discard(websocket)


async def broadcast_state(state: dict):
    global last_broadcast_state
    last_broadcast_state = state
    msg = json.dumps(state)
    disconnected = set()
    for ws in list(authenticated_clients):
        try:
            await ws.send(msg)
        except Exception:
            disconnected.add(ws)
    authenticated_clients.difference_update(disconnected)
    connected_clients.difference_update(disconnected)


async def poll_ppt_state():
    global last_broadcast_state
    while not shutdown_event.is_set():
        await asyncio.sleep(0.25)
        try:
            state = ppt.get_state()
            if state != last_broadcast_state and authenticated_clients:
                await broadcast_state(state)
        except Exception:
            pass


# ---------- Tkinter QR window (runs in a separate thread) ----------

shutdown_event = threading.Event()


def show_qr_window(ip: str, port: int, pin: str):
    uri = f"ws://{ip}:{port}?pin={pin}"
    qr_img = qrcode.make(uri)

    root = tk.Tk()
    root.title("Presenter Remote")
    root.resizable(False, False)

    frame = tk.Frame(root, padx=20, pady=20)
    frame.pack()

    tk.Label(frame, text="Presenter Remote Companion", font=("Segoe UI", 14, "bold")).pack(pady=(0, 10))
    tk.Label(frame, text=f"IP:   {ip}", font=("Consolas", 12)).pack(anchor="w")
    tk.Label(frame, text=f"Port: {port}", font=("Consolas", 12)).pack(anchor="w")
    tk.Label(frame, text=f"PIN:  {pin}", font=("Consolas", 16, "bold"), fg="#2266cc").pack(pady=(5, 10))

    buf = BytesIO()
    qr_img.save(buf, format="PNG")
    buf.seek(0)
    pil_img = Image.open(buf).resize((250, 250), Image.NEAREST)
    tk_img = ImageTk.PhotoImage(pil_img)

    label = tk.Label(frame, image=tk_img)
    label.image = tk_img
    label.pack(pady=(0, 5))

    tk.Label(frame, text="Scan with the Presenter Remote app", font=("Segoe UI", 10), fg="#666").pack()

    def on_close():
        shutdown_event.set()
        root.destroy()

    root.protocol("WM_DELETE_WINDOW", on_close)
    root.mainloop()


# ---------- Main ----------

async def main():
    ip = get_lan_ip()
    uri = f"ws://{ip}:{PORT}?pin={PIN}"

    print("=" * 50)
    print("  Presenter Remote Companion")
    print("=" * 50)
    print(f"  LAN IP : {ip}")
    print(f"  Port   : {PORT}")
    print(f"  PIN    : {PIN}")
    print(f"  URI    : {uri}")
    print("=" * 50)
    print("Waiting for connections...")

    gui_thread = threading.Thread(target=show_qr_window, args=(ip, PORT, PIN), daemon=True)
    gui_thread.start()

    server = await websockets.serve(handler, "0.0.0.0", PORT)
    try:
        await poll_ppt_state()
    finally:
        server.close()
        await server.wait_closed()


if __name__ == "__main__":
    import sys
    import os
    import traceback

    if sys.stdout is None:
        sys.stdout = open(os.devnull, "w")
    if sys.stderr is None:
        sys.stderr = open(os.devnull, "w")

    log_path = os.path.join(os.path.dirname(sys.executable) if getattr(sys, 'frozen', False) else os.path.dirname(__file__), "error.log")
    try:
        import pythoncom
        pythoncom.CoInitialize()
        asyncio.run(main())
    except Exception:
        err = traceback.format_exc()
        try:
            with open(log_path, "w") as f:
                f.write(err)
        except Exception:
            pass
        try:
            import tkinter.messagebox as mb
            mb.showerror("Presenter Companion Error", err)
        except Exception:
            pass
        sys.exit(1)
