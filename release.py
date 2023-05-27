import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).parent


def main():
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    match = re.match(r"version\s+=\s+(\d+)\.(\d+)\.(\d+)-SNAPSHOT", properties)
    version = match.group(1), match.group(2), match.group(3)

    release_version = '.'.join(version)
    new_version = '.'.join((version[0], str(int(version[1]) + 1), "0"))

    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    readme = re.sub(
        r"<version>[^<]+</",
        f"<version>{release_version}</",
        readme)
    readme = re.sub(
        r"de.richardliebscher.mdf4j:mdf4j:[^']+",
        f"de.richardliebscher.mdf4j:mdf4j:{release_version}",
        readme)
    (ROOT / "README.md").write_text(readme, encoding="utf-8")

    subprocess.call([
        "./gradlew", "release", "-Prelease.useAutomaticVersion=true",
        f"-Prelease.releaseVersion={release_version}",
        f"-Prelease.newVersion={new_version}-SNAPSHOT",
    ])


if __name__ == '__main__':
    main()
