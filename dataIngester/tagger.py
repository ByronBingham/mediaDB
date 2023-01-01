from subprocess import Popen, PIPE

process = Popen(['deepdanbooru', 'evaluate', 'E:/Pictures/__Saved Pictures/2008/146316.jpg', '--project-path', './DeepDanbooru'], stdout=PIPE, stderr=PIPE)
stdout, stderr = process.communicate()
print(stdout)
print(stderr)