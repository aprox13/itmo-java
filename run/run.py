import argparse
import glob
import json
import os
import re
import subprocess
import sys
import time

WORKING_DIR = os.environ['JAVA_ITMO']

USER = ['ru', 'ifmo', 'rain']
KORNEEV = ['info', 'kgeorgiy', 'java', 'advanced']

parser = argparse.ArgumentParser()
parser.add_argument('-hw', '--homework', metavar='name', type=str, default=None, help='homework num')
parser.add_argument('-u', '--user', metavar='name', type=str, default='belyaev', help='user name package')
parser.add_argument('-p', '--package', metavar='name', type=str, default=None, help='current task package')
parser.add_argument('-t', '--test', metavar='name', type=str, default=None, help='current test class')
parser.add_argument('-s', '--solve', metavar='name', nargs='+', type=str, default=None, help='solve classes')
parser.add_argument('--split', action='store_true', help='solve classes will be split in java execute')
parser.add_argument('-cp', '--classpath', metavar='paths', type=str, default='.', help='paths separated by \';\'')
parser.add_argument('--jar', action='store_true', help='create jar')
args = parser.parse_args()


def package_or_path(path: list, additional: list, sep: str) -> str:
    return sep.join(path + additional)


# noinspection PyShadowingNames
def get_javac(user: str, package: str, solve: str) -> str:
    return package_or_path(KORNEEV, [package],
                           os.sep) + os.sep + '*.java ' + package_or_path(USER, [user, package, solve],
                                                                          os.sep) + '.java'


def get_java_args_and_tests(cp: str, package: str, test: str) -> str:
    return '-cp "' + cp + '" -p . -m ' + package_or_path(KORNEEV, [package], '.') + ' ' + test


def canonical_name(main_package: list, spec_package: list) -> str:
    return package_or_path(main_package, spec_package, '.')


# noinspection PyShadowingNames
def get_java(cp: str, user: str, package: str, test: str, solve: str) -> str:
    if solve is str:
        return get_java_args_and_tests(cp, package, test) + ' ' + canonical_name(USER, [user, package, solve])

    names = []
    for cls in solve:
        names.append(canonical_name(USER, [user, package, cls]))
    return get_java_args_and_tests(cp, package, test) + ' ' + ','.join(names)


def get_user(separator: str) -> str:
    return separator.join(USER)


def max_hw_num() -> int:
    files = [os.path.basename(file) for file in
             glob.glob(package_or_path([WORKING_DIR], ['hw', 'run', '*.sh'], os.sep))]
    max_hw = 0
    for file in files:
        runnum = re.search(r'run\d+.sh', file)
        if runnum is not None:
            num = int(re.search(r'\d+', runnum.group(0)).group(0))
            if num > max_hw:
                max_hw = num
    return max_hw


def prt_tree():
    for root, dirs, files in os.walk("."):
        path = root.split(os.sep)
        print((len(path) - 1) * '---', os.path.basename(root))
        for file in files:
            print(len(path) * '---', file)


def clean():
    for root, dirs, files in os.walk(package_or_path([WORKING_DIR, 'hw', 'run'], [], os.sep)):
        for file in files:
            if file.endswith('.class'):
                os.remove(os.path.join(root, file))


def get_hw(hw_arg: str):
    hws = json.load(open("hws"))


def get_by_name(name: str) -> str:
    with open("hws", 'r') as file:
        hws = json.load(file)
        runs = hws['runs']

        target = runs[-1]
        if name is not None:
            for run in runs:
                if run['name'] == name:
                    target = run
                    break
        #       print(target)

        parser = argparse.ArgumentParser()

        parser.add_argument('--doc', action="store_true")
        parser.add_argument('--script', action='store_true')
        parser.add_argument("--myself", action='store_true')
        parser.add_argument('--split', action='store_true')
        parser.add_argument('--private', action='store_true')
        parser.add_argument('--jar', action='store_true')
        parser.add_argument('--classpath', type=str, default='.')

        args = parser.parse_args(target['params'])

        if args.script:
            subprocess.run(target['test'])
            return ""

        if args.doc:
            line = "python genDoc.py --package " + target['package'] + " --classes " + " ".join(target["solve"])
            if args.myself:
                line += ' --myself'
            if args.private:
                line += ' --private'
            subprocess.run(line)
            return ""

        result = "--user {0} --package {1} --test {2} --solve {3}".format(hws['user'], target['package'],
                                                                          target['test'],
                                                                          " ".join(target['solve']))

        if args.jar:
            result += ' --jar'
        if args.split:
            result += ' --split'

        result += ' --classpath ' + args.classpath

        return result


if args.solve is None:

    run = get_by_name(args.homework)
    if len(run) != 0:
        subprocess.run("python run.py " + run)
    # hw_num = max_hw_num() if args.homework == -1 else args.homework
    # subprocess.run('sh run' + str(hw_num) + '.sh' + (' --jar' if args.jar else ''))  # TODO
    # subprocess.run()
else:

    user = args.user
    package = args.package
    tests = args.test
    solve = args.solve

    for cls in solve:
        subprocess.run('javac ' + get_javac(user, package, cls))

    if args.jar:
        f = open('Manifest.txt', 'w')
        f.write('Manifest-Version: 1.0\n')
        f.write('Main-Class: ' + package_or_path(
            USER,
            [user,
             package,
             solve[0]],
            '.') + '\n')
        f.close()

        try:
            os.makedirs('artifacts' + os.sep + package)
        except OSError:
            pass
        create = 'jar cfm ' + package_or_path([], ['artifacts', package, solve[0] + '.jar'], os.sep) + ' Manifest.txt '
        create += package_or_path(KORNEEV, [package, '*.class'], os.sep)
        create += ' ' + package_or_path(USER, [user, package, '*.class'], os.sep)
        subprocess.run(create)
        os.remove('Manifest.txt')
    else:
        if args.split:
            for cls in solve:
                subprocess.run('java ' + get_java(args.classpath, user, package, tests, cls))
        else:
            subprocess.run('java ' + get_java(args.classpath, user, package, tests, solve))

clean()
