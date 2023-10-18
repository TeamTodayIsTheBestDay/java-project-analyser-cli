#!/usr/bin/env zsh

wget https://gitee.com/mirrors/oh-my-zsh/raw/master/tools/install.sh -O install.sh
chmod +x install.sh

export REPO=mirrors/oh-my-zsh
export REMOTE=https://gitee.com/${REPO}.git

zsh install.sh

git clone https://gitee.com/naoano/zsh-syntax-highlighting.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting
git clone https://gitee.com/zhengbangbo/zsh-autosuggestions.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions

sed 's/plugins=(git)/plugins=(git zsh-syntax-highlighting zsh-autosuggestions)/' ~/.zshrc

source /root/.zshrc
