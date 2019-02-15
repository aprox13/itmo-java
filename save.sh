( cd "run/" && sh clear.sh )
git add .
git status
git commit -m "$@"
git push origin master