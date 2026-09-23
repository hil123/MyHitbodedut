#!/usr/bin/env python3
"""Drive the installed MyHitbodedut APK on an emulator and capture real screens.

Every step taps an element found by its on-screen text in the live UI
hierarchy (uiautomator), so each screenshot is reached the way a user would.
A missing element is logged, never fatal: the log is part of the evidence.
"""
import os, re, subprocess, sys, time, xml.etree.ElementTree as ET

PKG = sys.argv[1] if len(sys.argv) > 1 else "com.myhitbodedut"
OUT = sys.argv[2] if len(sys.argv) > 2 else "capture"
SUFFIX = sys.argv[3] if len(sys.argv) > 3 else ""
os.makedirs(OUT, exist_ok=True)
LOG = open(os.path.join(OUT, f"log{SUFFIX}.txt"), "a")
counter = [0]


def log(msg):
    print(msg, flush=True)
    LOG.write(msg + "\n"); LOG.flush()


def adb(*args, binary=False):
    r = subprocess.run(["adb", *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode(errors="replace")


def sh(cmd):
    return adb("shell", cmd)


def dump():
    for _ in range(3):
        sh("uiautomator dump /sdcard/ui.xml >/dev/null 2>&1")
        raw = adb("exec-out", "cat /sdcard/ui.xml")
        if raw.strip().startswith("<?xml"):
            try:
                return raw, ET.fromstring(raw)
            except ET.ParseError:
                pass
        time.sleep(1)
    return "", None


def texts(root):
    out = []
    for n in root.iter("node"):
        t = n.get("text") or n.get("content-desc")
        if t:
            out.append(t)
    return out


def shot(name):
    counter[0] += 1
    base = f"{counter[0]:02d}-{name}{SUFFIX}"
    time.sleep(1.5)
    png = adb("exec-out", "screencap -p", binary=True)
    open(os.path.join(OUT, base + ".png"), "wb").write(png)
    raw, root = dump()
    if raw:
        open(os.path.join(OUT, base + ".xml"), "w").write(raw)
        activity = sh("dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity'").strip()
        log(f"[shot] {base}  ({activity})")
        log("        texts: " + " | ".join(texts(root))[:1500])
    else:
        log(f"[shot] {base} (no ui dump)")


def bounds_center(b):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", b))
    return (x1 + x2) // 2, (y1 + y2) // 2


def find(pattern, root):
    rx = re.compile(pattern, re.I)
    for n in root.iter("node"):
        for key in ("text", "content-desc"):
            v = n.get(key) or ""
            if v and rx.search(v):
                return n
    return None


def tap(pattern, scroll=True, required=True):
    for attempt in range(8 if scroll else 1):
        _, root = dump()
        if root is not None:
            n = find(pattern, root)
            if n is not None:
                x, y = bounds_center(n.get("bounds"))
                sh(f"input tap {x} {y}")
                log(f"[tap] /{pattern}/ -> ({x},{y}) '{n.get('text') or n.get('content-desc')}'")
                time.sleep(2)
                return True
        if scroll:
            swipe_up()
    (log if required else (lambda m: None))(f"[miss] /{pattern}/ not found")
    return False


def size():
    m = re.search(r"(\d+)x(\d+)", sh("wm size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


def swipe_up(frac=0.55):
    w, h = size()
    sh(f"input swipe {w//2} {int(h*0.78)} {w//2} {int(h*(0.78-frac))} 450")
    time.sleep(1.2)


def to_top():
    w, h = size()
    for _ in range(6):
        sh(f"input swipe {w//2} {int(h*0.25)} {w//2} {int(h*0.85)} 200")
    time.sleep(1)


def back():
    sh("input keyevent 4"); time.sleep(1.5)


def home_screen():
    sh(f"am start -n {PKG}/com.myhitbodedut.MainActivity >/dev/null")
    time.sleep(2.5)
    to_top()


def scroll_shots(name, n=4):
    shot(f"{name}-a")
    for i in range(1, n):
        swipe_up(0.5)
        shot(f"{name}-{chr(97+i)}")


def main():
    mode = os.environ.get("MODE", "full")
    sh(f"am start -n {PKG}/com.myhitbodedut.MainActivity >/dev/null")
    time.sleep(5)

    if mode == "full":
        # First launch: onboarding
        for i in range(1, 4):
            shot(f"onboarding-{i}")
            if not tap(r"^(Next|Begin)$", scroll=False):
                break
        time.sleep(2)
        to_top()
        scroll_shots("home", 6)

        # Settings: turn the guided hitbodedut on, look at the gate preview
        home_screen()
        tap(r"^Settings$")
        scroll_shots("settings", 7)
        to_top()
        tap(r"^Guided hitbodedut$")
        shot("settings-guided-on")
        if tap(r"Preview the gate screen now"):
            time.sleep(2)
            shot("gate-preview")
            tap(r"Close the preview", scroll=False, required=False)
        back()

        # Session
        home_screen()
        tap(r"^Start a .* session$")
        shot("session-idle")
        tap(r"^Subject", scroll=False, required=False) and (shot("session-subjects"), tap(r"^Gratitude$", scroll=False))
        tap(r"^10 min", scroll=False) or tap(r"^Start, ", scroll=False)
        time.sleep(3)
        shot("session-running")
        swipe_up(0.4); shot("session-running-scrolled")
        for step in range(2, 6):
            if tap(r"^Next$", scroll=False, required=False):
                shot(f"session-guide-{step}")
        tap(r"I cannot find the words")
        shot("session-stuck")
        to_top()
        tap(r"^Finish now$")
        shot("session-finish-dialog")
        tap(r"^Finish now$", scroll=False)
        time.sleep(3)
        shot("session-after")
        if tap(r"^Write a note$", required=False) or find(r"Session note", dump()[1] or ET.Element("x")) is not None:
            shot("note-dialog")
            sh("input text 'Thank%syou.'")
            shot("note-dialog-typed")
            tap(r"^Save$", scroll=False)
        home_screen()
        scroll_shots("home-done", 3)

        # Journal
        home_screen()
        tap(r"^Open the journal$")
        shot("journal")
        back()

    # Content sections (both modes)
    for label, name, deeper in [
        (r"^Tikkun HaKlali$", "tikkun", r"16"),
        (r"^Tfilot$", "tefilot", r"Hitbodedut"),
        (r"^Library$", "library", None),
        (r"^Tips for Hitbodedut$", "tips", None),
        (r"^Reminders$", "reminders", None),
        (r"^Music$", "music", None),
    ]:
        home_screen()
        if not tap(label):
            continue
        scroll_shots(name, 3 if mode == "full" else 1)
        if deeper and mode == "full":
            to_top()
            if tap(deeper, required=False):
                scroll_shots(f"{name}-detail", 2)
                back()
        if name == "library" and mode == "full":
            to_top()
            _, root = dump()
            # open the first clickable list entry below the header
            nodes = [n for n in root.iter("node") if n.get("clickable") == "true"] if root is not None else []
            if nodes:
                x, y = bounds_center(nodes[min(1, len(nodes)-1)].get("bounds"))
                sh(f"input tap {x} {y}"); time.sleep(2)
                scroll_shots("library-section", 2)
                _, root = dump()
                nodes = [n for n in root.iter("node") if n.get("clickable") == "true"] if root is not None else []
                if nodes:
                    x, y = bounds_center(nodes[min(1, len(nodes)-1)].get("bounds"))
                    sh(f"input tap {x} {y}"); time.sleep(2)
                    scroll_shots("library-entry", 2)
        if name == "tikkun" and mode == "full":
            home_screen(); tap(label)
            if tap(r"Read it all in one flow", required=False):
                scroll_shots("tikkun-flow", 2)
        back()

    home_screen()
    shot("home-final")
    log("[done]")


if __name__ == "__main__":
    main()
