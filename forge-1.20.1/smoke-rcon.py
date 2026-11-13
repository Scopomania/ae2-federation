#!/usr/bin/env python3
"""Tiny RCON client: rcon.py HOST PORT PASSWORD COMMANDS_FILE  -> prints each command and the server's reply."""
import socket, struct, sys, time

def recvn(sock, n):
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("rcon connection closed")
        data += chunk
    return data

def send(sock, request_id, kind, body):
    payload = body.encode()
    sock.sendall(struct.pack("<iii", 10 + len(payload), request_id, kind) + payload + b"\x00\x00")

def read(sock):
    length = struct.unpack("<i", recvn(sock, 4))[0]
    data = recvn(sock, length)
    request_id, kind = struct.unpack("<ii", data[:8])
    return request_id, kind, data[8:-2].decode(errors="replace")

def command(sock, text):
    send(sock, 7, 2, text)
    _, _, reply = read(sock)
    return reply

def main(host, port, password, path):
    sock = None
    for _ in range(60):
        try:
            sock = socket.create_connection((host, int(port)), timeout=30)
            break
        except OSError:
            time.sleep(2)
    if sock is None:
        print("RCON: could not connect"); return 2
    send(sock, 1, 3, password)
    request_id, _, _ = read(sock)
    if request_id == -1:
        print("RCON: wrong password"); return 2
    failures = 0
    reply = ""
    for line in open(path):
        line = line.strip()
        if line.startswith("#expect "):
            # "#expect <text>": the previous command's reply must contain <text>
            wanted = line[len("#expect "):]
            ok = wanted in reply
            print(f"    expect {wanted!r}: {'ok' if ok else 'FAILED'}")
            if not ok:
                failures += 1
            continue
        if not line or line.startswith("#"):
            continue
        reply = command(sock, line)
        print(f"> {line}\n    {reply or '(no output)'}")
        if any(bad in reply for bad in ("Found no block entity", "Unknown", "Incorrect argument", "Expected", "Could not", "Invalid")):
            failures += 1
        time.sleep(2)   # let the server tick between commands
    print(f"RCON: {failures} command(s) reported a problem")
    command(sock, "save-all")
    try:
        send(sock, 8, 2, "stop")
    except OSError:
        pass
    return 1 if failures else 0

if __name__ == "__main__":
    sys.exit(main(*sys.argv[1:5]))
