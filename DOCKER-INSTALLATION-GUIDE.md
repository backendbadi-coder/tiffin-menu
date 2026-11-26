# Docker Installation Guide (Windows 11 / 10 with WSL2)

This guide installs **Docker** and **Docker Compose** so you can run the Tiffin Menu API (Spring Boot 4.2 + PostgreSQL) with `docker compose up`.

**Choose ONE option. Don't install both: they conflict over the `docker` command.**

| | **Option A: Docker Desktop** | **Option B: Docker Engine inside WSL2** |
|---|---|---|
| Best for | Beginners, people who like a GUI | People comfortable with a Linux terminal |
| GUI | ✅ Containers, images, volumes in a window | ❌ Terminal only |
| `docker` from PowerShell | ✅ | ❌ (only inside Ubuntu) |
| Cost | Free for personal use, education and small companies; **paid for larger companies** (see below) | Free and open source |
| Effort | Installer, a few clicks | About 10 terminal commands |

**How to read this guide**
- ✅ = from the official Docker or Microsoft documentation.
- ⚠️ = depends on your Windows/WSL version, or not confirmed in the official docs. If what you see differs, follow the linked official page.

---

## Step 0. Prepare Windows (needed for both options)

### 0.1 Turn on virtualization
Docker needs hardware virtualization. Open **Task Manager → Performance → CPU** and check that **Virtualization: Enabled** is shown. If it says *Disabled*, turn it on in your PC's BIOS/UEFI (the setting is usually called *Intel VT-x*, *Intel Virtualization Technology* or *AMD-V / SVM*). The exact menu depends on your PC maker ⚠️.

### 0.2 Install or update WSL2 (*PowerShell*)
```powershell
wsl --version
```
- **Command not recognized** → install WSL with Ubuntu (restart Windows if asked):
  ```powershell
  wsl --install -d Ubuntu
  ```
- **Version shown** → update to the latest:
  ```powershell
  wsl --update
  ```

Check that your distribution uses WSL **2**:
```powershell
wsl --list --verbose
```
If `VERSION` shows `1`, convert it:
```powershell
wsl --set-version Ubuntu 2
wsl --set-default-version 2
```

---

## Option A: Docker Desktop for Windows

### A1. Check the requirements ✅
- WSL 2 turned on, and a recent WSL version (Docker's WSL page asks for **at least WSL 2.1.5**, ideally the latest) ✅
- 64-bit processor with SLAT, and **8 GB RAM** ✅
- Virtualization enabled (Step 0.1) ✅
- A supported Windows 10/11 build ⚠️ (the minimum build changes over time; check the *System requirements* section on Docker's install page)

**License:** commercial use of Docker Desktop in larger enterprises (**more than 250 employees OR more than USD 10 million annual revenue**) requires a paid subscription ✅. Personal learning is fine. If you use a company laptop, ask your IT team first.

**Already installed Docker Engine inside WSL (Option B) before?** Uninstall it first, because running both causes conflicts ✅ (see "Uninstall" at the end).

### A2. Install
1. Download **Docker Desktop for Windows** from https://docs.docker.com/desktop/setup/install/windows-install/
2. Double-click **Docker Desktop Installer.exe**.
3. On the configuration page, keep **"Use WSL 2 instead of Hyper-V"** selected ✅.
4. Finish the installer and restart Windows if asked.
5. Start **Docker Desktop** from the Start menu and accept the terms. Signing in to a Docker account is optional for personal use ⚠️.

### A3. Turn on WSL integration ✅
In Docker Desktop: **Settings (⚙) → General** → make sure **Use WSL 2 based engine** is checked.
Then **Settings → Resources → WSL integration** → turn on your **Ubuntu** distribution → **Apply & restart**.

Now the `docker` command works in **PowerShell** and inside **Ubuntu**.

### A4. Verify → go to [Step V](#step-v-verify-the-installation-both-options)

---

## Option B: Docker Engine inside WSL2 Ubuntu

### B1. Make sure systemd is on
systemd lets Docker start automatically. Ubuntu installed with `wsl --install` already uses systemd by default ✅. Check inside Ubuntu:
```bash
cat /etc/wsl.conf
```
If you **don't** see `systemd=true`, add it:
```bash
sudo tee -a /etc/wsl.conf <<'EOF'
[boot]
systemd=true
EOF
```
Then in *PowerShell* run `wsl --shutdown`, and open Ubuntu again. (systemd needs WSL 0.67.6 or newer ✅; `wsl --update` from Step 0.2 covers this.)

### B2. Remove old or unofficial Docker packages
It's fine if apt reports that none are installed:
```bash
sudo apt remove $(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc docker-buildx podman-docker containerd runc | cut -f1)
```

### B3. Add Docker's official apt repository ✅
```bash
sudo apt update
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
```

### B4. Install Docker Engine, Buildx and Compose ✅
```bash
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

### B5. Use Docker without `sudo`
```bash
sudo usermod -aG docker $USER
exit
```
Open Ubuntu again (the group change only applies to a new login).

> Security note: members of the `docker` group effectively have root access on that machine. That's fine on your own development PC ✅ (Docker post-install docs).

### B6. Make sure Docker is running
```bash
sudo systemctl status docker        # should say "active (running)"
sudo systemctl enable --now docker  # only if it isn't running
```

---

## Step V. Verify the installation (both options)

Run in Ubuntu (or PowerShell for Option A):
```bash
docker version
docker compose version
docker run hello-world
```

**Expect**
- `docker version` shows **both** a `Client` and a `Server` section.
- `docker compose version` prints `Docker Compose version v2.x` (or newer).
- `hello-world` prints **"Hello from Docker!"**.

> Use `docker compose` (with a **space**, Compose v2). The old `docker-compose` (with a hyphen) is the legacy tool.

---

## Step T. Try it with the Tiffin Menu project

Clone the project **inside the Linux file system** (much faster than `/mnt/c/...`) ⚠️:
```bash
mkdir -p ~/projects && cd ~/projects
git clone https://github.com/backendbadi-coder/tiffin-menu.git
cd tiffin-menu

cp .env.example .env          # then open .env and set your own DB_PASSWORD
docker compose up -d --build
docker compose ps             # db: healthy, app: running
curl http://127.0.0.1:8080/api/menu-items
```
Stop everything with `docker compose down` (add `-v` to also delete the database data).

> ⚠️ `.env.example` and `compose.yaml` must exist in the repository for these commands. If they aren't there yet, check the repository README for the current instructions.

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Docker Desktop: *"WSL 2 installation is incomplete"* / *"WSL update required"* | Old WSL version | PowerShell: `wsl --update`, then restart Docker Desktop |
| Docker Desktop: *"Virtualization support not detected"* | Virtualization disabled | Turn it on in BIOS/UEFI (Step 0.1) |
| `docker: command not found` in Ubuntu (Option A) | WSL integration off | Docker Desktop → Settings → Resources → WSL integration → turn on Ubuntu |
| `permission denied while trying to connect to the Docker daemon socket` (Option B) | Not in the `docker` group yet, or no re-login | `sudo usermod -aG docker $USER`, `exit`, reopen Ubuntu (or `wsl --shutdown`) |
| `Cannot connect to the Docker daemon ... Is the docker daemon running?` | Option A: Docker Desktop not started. Option B: service stopped / systemd off | A: start Docker Desktop. B: `sudo systemctl start docker`; check B1 |
| Strange errors after trying both options | Docker Desktop and Docker Engine-in-WSL installed together | Keep only one: uninstall the other (see below) |
| `required variable DB_PASSWORD is missing a value` | `.env` missing, in the wrong folder, empty, or saved as `.env.txt` | `cp .env.example .env` in the project folder and set `DB_PASSWORD=...` |
| `port is already allocated` | Something else uses the port (old container, IntelliJ app) | `docker ps`, `docker rm -f <name>`, or stop the app in IntelliJ |
| Windows browser can't open `localhost:8080` (Option B) | WSL localhost forwarding off ⚠️ | Test with `curl` in Ubuntu first; `wsl --shutdown` and try again |
| Builds are very slow | Project is under `/mnt/c/...` | Move it to `~/projects` |

---

## Uninstall

**Option A (Docker Desktop):** Windows **Settings → Apps → Installed apps → Docker Desktop → Uninstall**.

**Option B (Docker Engine in WSL):**
```bash
sudo apt purge docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin docker-ce-rootless-extras
sudo rm -rf /var/lib/docker /var/lib/containerd   # deletes ALL images, containers and volumes
sudo rm /etc/apt/sources.list.d/docker.sources /etc/apt/keyrings/docker.asc
```

---

## macOS and Linux users
- **macOS:** install **Docker Desktop for Mac** (choose Apple silicon or Intel): https://docs.docker.com/desktop/setup/install/mac-install/
- **Ubuntu / Linux:** follow Option B from step B2 onwards (no WSL steps). Other distributions: https://docs.docker.com/engine/install/

---

## Sources
- Docker: Install Docker Desktop on Windows (requirements, installer, license terms): https://docs.docker.com/desktop/setup/install/windows-install/
- Docker: Docker Desktop WSL 2 backend on Windows (WSL 2.1.5+, WSL integration, uninstall Engine-in-WSL first): https://docs.docker.com/desktop/features/wsl/
- Docker: Install Docker Engine on Ubuntu (apt repository, packages, uninstall): https://docs.docker.com/engine/install/ubuntu/
- Docker: Linux post-installation steps (`docker` group): https://docs.docker.com/engine/install/linux-postinstall/
- Microsoft: Install WSL: https://learn.microsoft.com/windows/wsl/install
- Microsoft: Advanced settings configuration in WSL (systemd): https://learn.microsoft.com/windows/wsl/wsl-config
- Microsoft: systemd in WSL: https://learn.microsoft.com/windows/wsl/systemd
