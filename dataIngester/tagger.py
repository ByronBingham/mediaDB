from subprocess import Popen, PIPE
from pathlib import Path


def run_deepdanbooru(image_paths) -> str:
    exe_path = str(Path("./venv/Scripts/python.exe").absolute())
    exe_path = exe_path.replace("\\", "/")
    path_string = "\"" + ("\" \"".join(image_paths)).replace("\\\\", "/").replace("\\", "/") + "\""
    cmd = [exe_path, "-m", "deepdanbooru", "evaluate", "--project-path", "./DeepDanbooru", "--allow-gpu"] + image_paths
    process = Popen(cmd, stdout=PIPE, stderr=PIPE)
    try:
        stdout, stderr = process.communicate()
    except Exception as e:
        print("ERROR: Error encountered running DeepDanbooru")
        print("ERROR: MESSAGE: \n" + str(e))
        print("DEBUG: DD was run with these files:")
        for path in image_paths:
            print("    " + str(path))

    if stderr != b'':
        stderr_string = stderr.decode('utf-8')
        if "Cleanup called" in stderr_string:
            print("DEBUG: Cleanup called in DeepDanbooru")
        elif "can't encode character" in stderr_string:
            print("ERROR: Something couldn't be encoded")
            print("DEBUG: DD was run with these files:")
            for path in image_paths:
                print("    " + str(path))
        else:
            print("ERROR: " + stderr.decode('utf-8'))
    return stdout

def parse_deepdanbooru_output(data: str, threshold: float):
    if data == '':
        return {}
    out = {}
    data = data.decode("utf-8")

    sections = data.split('Tags of ')
    for section in sections:
        if section == '':
            continue
        lines = section.splitlines(keepends=False)

        tags_with_probs = []
        for line in lines:
            if not line.startswith('('):
                continue
            line = line.replace('(', '')
            line = line.replace(')', '')
            [prob, tag] = line.split(' ')
            tmp = (tag, float(prob))
            if tmp[1] >= threshold:
                tags_with_probs.append(tmp)

        out[str((Path(lines[0][:-1])).resolve())] = tags_with_probs

    return out

def get_filtered_tags(image_paths, threshold: float):
    return parse_deepdanbooru_output(run_deepdanbooru(image_paths=image_paths), threshold=threshold)
