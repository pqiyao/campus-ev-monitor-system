import os
import sys
import time

import paramiko


def required_env(name):
    value = os.environ.get(name)
    if not value:
        raise RuntimeError("missing environment variable: " + name)
    return value


host = required_env("EV_SERVER_HOST")
username = required_env("EV_SERVER_USER")
password = required_env("EV_SERVER_PASSWORD")
archive = required_env("EV_STATIC_ARCHIVE")
remote_root = "/opt/ev-monitor/deploy"
stamp = time.strftime("%Y%m%d%H%M%S")
remote_archive = "/tmp/ev-monitor-dist-" + stamp + ".tar.gz"
new_dir = remote_root + "/dist_new_" + stamp
backup_dir = remote_root + "/dist_backup_" + stamp

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(
    host,
    username=username,
    password=password,
    timeout=15,
    auth_timeout=15,
    banner_timeout=15,
)

try:
    with client.open_sftp() as sftp:
        sftp.put(archive, remote_archive)

    command = """set -eu
mkdir -p '{new_dir}'
tar -xzf '{remote_archive}' -C '{new_dir}'
test -f '{new_dir}/index.html'
test -d '{new_dir}/assets'
if [ -d '{root}/dist' ]; then mv '{root}/dist' '{backup_dir}'; fi
mv '{new_dir}' '{root}/dist'
cd '{root}'
docker compose up -d --force-recreate web
rm -f '{remote_archive}'
curl --fail --silent --show-error --max-time 15 http://127.0.0.1/index.html >/dev/null
curl --fail --silent --show-error --max-time 15 http://127.0.0.1/img/vehicle-card.svg >/dev/null
echo DEPLOY_OK
echo BACKUP={backup_dir}
""".format(
        new_dir=new_dir,
        remote_archive=remote_archive,
        root=remote_root,
        backup_dir=backup_dir,
    )
    stdin, stdout, stderr = client.exec_command(command, timeout=120)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    if out:
        print(out, end="")
    if err:
        print(err, file=sys.stderr, end="")
    if exit_code != 0:
        raise RuntimeError("remote deployment failed with exit code " + str(exit_code))
finally:
    client.close()
