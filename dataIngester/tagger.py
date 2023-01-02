from subprocess import Popen, PIPE
from pathlib import Path


def run_deepdanbooru(image_path: str) -> str:
    exe_path = str(Path("./venv/Scripts/python.exe").absolute())
    exe_path = exe_path.replace("\\", "/")
    cmd = "deepdanbooru evaluate \"" + image_path + "\" --project-path ./DeepDanbooru"
    print(cmd)
    process = Popen(cmd, stdout=PIPE, stderr=PIPE)
    stdout, stderr = process.communicate()
    if stderr != b'':
        print("ERROR: " + str(stderr))
    return stdout


def parse_deepdanbooru_output(data: str):
    if data == '':
        return []
    lines = data.splitlines(keepends=False)

    tags_with_probs = []
    for line in lines:
        line = line.decode("utf-8")
        if not line.startswith('('):
            continue
        line = line.replace('(', '')
        line = line.replace(')', '')
        [prob, tag] = line.split(' ')
        tags_with_probs.append((tag, prob))
    return tags_with_probs


def filter_tags(tags_with_probs, threshold: float):
    filtered_tags = []
    for pair in tags_with_probs:
        if float(pair[1]) > threshold:
            filtered_tags.append(pair[0])

    return filtered_tags

# print(filter_tags(parse_deepdanbooru_output(run_deepdanbooru(
#    "./test_data/anime_art/0dvp198ybus91.jpg")), 0.9))
