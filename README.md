<p align="center"><h1>oscm-bug-migration</h1></p> 
<p>This tool migrates all bug entries from given Bugzilla locaton to GitLab including all conversation histories. 
</p>
<h3>Prerequesites</h3>

Bugzilla and Gitlab are online and accessible with same credentials given by the user.  

<h3>Usage</h3>

1. Edit `config/config.propterties`

2. Run ```mvn clean package```

3. Open Windows Command Prompt at `oscm-bug-migration/target`

4. Run `oscm-bug-migration-0.0.1-SNAPSHOT-jar-with-dependencies.jar -u <username>;`

