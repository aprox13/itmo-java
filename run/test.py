import argparse
import json
import subprocess


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


def get_names() -> list:
    with open("hws", 'r') as file:
        hws = json.load(file)
        runs = hws['runs']

        res = []

        for run in runs:
            res.append(run['name'])
        return res


# print(get_names())


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


