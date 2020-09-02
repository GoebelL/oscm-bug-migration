<p align="center"><h1>oscm-bug-migration</h1></p> 
<p>This tool migrates all bug entries from a given Bugzilla to a GitLab repository including all conversation histories and file attachments. 
</p>
<h3>Prerequesites</h3>
<p>
Bugzilla and Gitlab are online and accessible with same credentials given by the user.  
</p>

<h3>Usage</h3>
<p>Usage is simple as follows<br>
  
1. Edit `config/config.properties` and fill the properties.   

2. Run ```mvn clean package```.  

3. Open a Windows Command Prompt at `oscm-bug-migration/target`.  

4. Run `oscm-bug-migration-0.0.1-SNAPSHOT-jar-with-dependencies.jar -u <username>.`  
</p>
<h3>Configuration</h3>

Leave **migration.productive** to `false` to make a test run. This doesn't import anything but only logs the import data to './log.json'.Finally rerun the tool with **migration.productive** `true` to import the issues in GitLab. Run with `-d` if you want to first delete all existing issues in the target GitLab project.

