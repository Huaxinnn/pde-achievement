#!/usr/bin/env bash
set -e

if [ ! -z "$APP_KEY" ]; then
    echo "start replace appkey...\n"
    find . -type f -name "app.properties" -exec sed -i '1s/app.name=.*/app.name='$APP_KEY'/g' {} +
fi

if [ -z "$ACTIVE_PROFILE" ]; then
    ACTIVE_PROFILE=$PLUS_TEMPLATE_ENV
fi
if [ -z "$ACTIVE_PROFILE" ]; then
    ACTIVE_PROFILE=test
fi

# Plus Build 平台禁止使用自定义 settings.xml
echo "Using standard Plus Build maven settings"

mvn clean package -U -P $ACTIVE_PROFILE -DskipTests=true -Dmaven.source.skip=true -Dsource.skip=true

# 将 deploy 脚本复制到 target 目录，供构建系统打包
mkdir -p ./target/deploy
cp -r ./deploy ./target/
