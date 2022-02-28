This plugin is not maintained anymore as from version 3.2.1 the maven-invoker-plugin support generating surefire/junit compatible. (see https://issues.apache.org/jira/browse/MINVOKER-196) 

You only need to setup invoker plugin with the following flags

```
<writeJunitReport>true</writeJunitReport>
<junitPackageName></junitPackageName>
```

`junitPackageName` can be used to have the its part of a package from the junit report.

Then configure `junit` step to scan those files

```
junit('**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml,**/target/invoker-reports/*.xml')
```


# maven-invoker-plugin
maven-invoker-plugin for jenkins

Use coding style from here http://maven.apache.org/developers/committer-environment.html
