#!/usr/bin/env bash
# One-time VM setup. Run ON the VM as a sudo-capable user:  bash provision-vm.sh
# Idempotent: safe to re-run.
set -euo pipefail

sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless nginx

# Swap file: the VM has ~1 GB RAM and no swap
if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 1G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

id salary &>/dev/null || sudo useradd --system --home /var/lib/salary --shell /usr/sbin/nologin salary
sudo mkdir -p /opt/salary /var/lib/salary /var/www/salary
sudo chown salary:salary /var/lib/salary
sudo chown ubuntu:ubuntu /opt/salary /var/www/salary   # deploy user writes here

sudo cp /tmp/salary-backend.service /etc/systemd/system/salary-backend.service
sudo systemctl daemon-reload
sudo systemctl enable salary-backend

sudo cp /tmp/nginx-salary.conf /etc/nginx/sites-available/salary
sudo ln -sf /etc/nginx/sites-available/salary /etc/nginx/sites-enabled/salary
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx

# Let the deploy user restart only this service without a password prompt
echo 'ubuntu ALL=(root) NOPASSWD: /bin/systemctl restart salary-backend, /bin/systemctl status salary-backend' | sudo tee /etc/sudoers.d/salary-deploy >/dev/null
sudo chmod 440 /etc/sudoers.d/salary-deploy
echo "Provisioning complete."
