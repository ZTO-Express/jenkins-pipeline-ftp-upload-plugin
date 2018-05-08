# ftp上传插件
对指定的文件上传到ftp上指定的目录下


# 生成插件
```
mvn clean package -DskipTests
```
最后会打包成一个hpi文件，这是jenkins的插件文件

# 安装插件
我们在jenkins的系统管理->管理插件->高级-上传插件中上传我们生成的hpi文件，上传成功后等待jenkins重启。

# 在pipeline中使用插件
```
stage('打包') {
    steps {
        script{
            file = zipFile source:"buildout",excludes:""
        }
    }
}
```
source：要打包的目录名称
excludes：文件排除规则

此插件只能用在pipeline风格的项目中