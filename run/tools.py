import argparse
import json
import os
import shutil
import subprocess

HOME = os.environ['JAVA_ITMO']

CODE_PATHS = [os.sep.join([HOME, 'hw', 'src', 'ru']), os.sep.join([HOME, 'hw', 'src', 'info'])]

SOURCE = '/'.join([HOME, 'source', 'java-advanced-2019'])

LIBS = '/'.join([SOURCE, 'lib', '*.jar'])
TEST = '/'.join([SOURCE, 'artifacts', '*.jar'])

RUN = os.sep.join([HOME, 'hw', 'run'])

parser = argparse.ArgumentParser()

parser.add_argument('-uc', '--update_code', action='store_true', help='copy all from ../src/ to run dir')
parser.add_argument('-ut', '--update_test', action='store_true', help='pull git & copy all here')
parser.add_argument('-ct', '--copy_test', action='store_true', help='copy tests here')
parser.add_argument('-u', '--update', action='store_true', help='full update')
parser.add_argument('-c', '--clean', action='store_true', help='remove all hw files from run dir')
parser.add_argument('--add', action='store_true', help='add new task')
parser.add_argument('--all', action='store_true', help='show all tasks')

args = parser.parse_args()


def add_task():
    with open("hws") as file:
        hws = json.load(file)

    print("Name: ", end='')
    name = input()
    print("Package: ", end='')
    package = input()
    print("Test(or script if --script exist): ", end='')
    test = input()

    print("Class(es): ", end='')
    classes = []
    for cls in input().split(" "):
        if len(cls) != 0:
            classes.append(cls)

    print("--doc    -> generate doc")
    print("--script -> run from test")
    print("--split,\n"
          "--jar , \n"
          "--classpath    \n"
          "--myself -> from run and genDoc")
    print('Params(can be empty): ', end='')

    params = []
    for p in input().split(" "):
        if len(p) != 0:
            params.append(p)

    result = {
        "name": name,
        "package": package,
        "test": test,
        "solve": classes,
        "params": params
    }

    hws['runs'].append(result)

    with open('hws', 'w') as wfile:
        json.dump(hws, wfile)


def copy_directory(src, dest):
    print("copy from " + src + ' to ' + dest)
    try:
        shutil.copy2(src, dest)
    # Directories are the same
    except shutil.Error as e:
        print('Directory not copied. Error: %s' % e)
    # Any error saying that the directory doesn't exist
    except OSError as e:
        f = open('log.txt', 'a')
        f.write(str(e) + '\n')
        print('Directory not copied1. Error: %s' % e)


def update_test():
    path = SOURCE.replace('/', os.sep)
    subprocess.run("sh {}{}upd.sh {}".format(path, os.sep, path))


def copy_test():
    subprocess.run("cp {0} {1}".format(LIBS, RUN))
    subprocess.run("cp {0} {1}".format(TEST, RUN))
    pass


def update_code():
    for code in CODE_PATHS:
        subprocess.run('cp -r "' + code + '" "' + RUN + '"')


if args.add:
    add_task()
elif args.all:
    with open("hws", 'r') as file:
        hws = json.load(file)
        runs = hws['runs']

        res = []

        for run in runs:
            print(run['name'])


elif args.clean:
    for root, dirs, files in os.walk(RUN):
        for file in files:
            if not file.endswith('.py') and not file.startswith("hws"):
                os.remove(os.path.join(root, file))
        for dir in ['doc', 'ru', 'info', 'artifacts']:
            os.rmdir(os.path.join(root, dir))
else:

    if args.update:
        update_test()
        copy_test()
        update_code()
    elif args.update_test:
        update_test()
        copy_test()
    elif args.copy_test:
        copy_test()

    if args.update_code:
        update_code()
