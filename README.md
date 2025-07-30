## Getting started

This project contains pre-commit git hook, which will be used to dynamically generate mockserver expectations based on json files under expectations folder when committing code. So before committing any code, you need to execute below command to enable the git hooks:
```shell
# Change the hook's executable status
chmod +x .hooks/pre-commit

# Configure git hooks folder
git config --global core.hooksPath .hooks
```
