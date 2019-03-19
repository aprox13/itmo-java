import argparse
import os
import subprocess

JAVA = os.environ['JAVA_ITMO']

ADDITIONAL = os.sep.join(['src', 'ru', 'rain'])


def get_self_path(package: str, cls: str) -> str:
    return os.sep.join([JAVA, 'hw', 'src', 'ru', 'ifmo', 'rain', 'belyaev', package, cls + '.java'])


def get_korneev_path(package: str) -> str:
    return os.sep.join([JAVA, 'hw', 'src', 'info', 'kgeorgiy', 'java', 'advanced', package, '*.java'])


parser = argparse.ArgumentParser()

parser.add_argument('-p', '--package', default=None, type=str, help='package of classes(will be created dir in doc/')
parser.add_argument('--myself', action='store_true', help='don\'t doc Korneev classes')
parser.add_argument('-c', '--classes', nargs='+', type=str, default=None, help='classes for doc')
parser.add_argument('--private', action='store_true', help='generate for private')

args = parser.parse_args()

classes = args.classes
package = args.package
myself = args.myself

if package is not None and classes is not None:
    print('classes ' + str(classes))
    print('package ' + package)

    result = 'javadoc -d doc' + os.sep + package
    for cls in classes:
        result += ' ' + get_self_path(package, cls)
    if not myself:
        result += ' ' + get_korneev_path(package)

    result += ' -link https://docs.oracle.com/en/java/javase/11/docs/api/'
    if args.private:
        result += ' -private'
    subprocess.run(result)
