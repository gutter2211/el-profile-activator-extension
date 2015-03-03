EL Profile Activation Maven Extension
-------------------------------------

This source was copied from https://github.com/kpiwko/el-profile-activator-extension

Allows profile to be activated using an expression language expression. Currently supports MVEL2 only. 
Extension hijacks property activation and tries to evaluate mvel expression first, if this is not successful
it passes control to original property activator.

In order to activate extension, you cannot include it into ```<build><extensions>``` element, because profile activation is done
before it would be activated. So you need to copy following files into *$MAVEN_HOME/lib/ext*:

* el-profile-activator-extension.jar (available in target directory)
* mvel2-${version} (available http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.mvel%22%20AND%20a%3A%22mvel2%22, substitute with latest version)  

To profit from new activation, add following in your pom.xml:

    <profile>
        <id>my-profile</id>
    
        <activation>
            <property>
            	<!-- mvel property name is obligatory -->
                <name>mvel</name>
                <value>isdef foo &amp;&amp; foo=="abc"</value>
            </property>
        </activation>
    </profile>            
    
A few examples (an MVEL cheatsheet)
-----------------------------------

* Check if *foo* and *bar* are defined and have same value 
 
		isdef foo &amp;&amp; isdef bar &amp;&amp; foo==bar
		
* Check if *foo* is defined while *bar* is not
		
	    isdef foo &amp;&amp; !isdef bar
	    
* Check if *foo* starts with *abc* or *baz* contains *xyz*
	
		isdef foo &amp;&amp; foo.startsWith("abc")) || (isdef baz &amp;&amp; baz.contains("xyz"))

Complete MVEL reference guide is available at http://mvel.codehaus.org/Language+Guide+for+2.0		


Erweiterung durch Comdirect
---------------------------

Als Variablen für die EL-Validierung stehen im Original-Code die System-Properties und Userdefined-Properties zur Verfügung. Leider 
stehen keine Properties zur Verfügung, die in Poms oder gar Child-Poms definiert, wie z.B. 

    <properties>
        <anyprop>anyval<anyprop>
    </properties>
    
Der Grund liegt darin begründet, dass die Entscheidung über die Aktivierung eines Profiles getroffen werden muss, *bevor* die Pom fertig 
aufgebaut ist, da in einem Profile ja Projekt-Properties noch verändert werden könnten.

Die Properties werden in der Commandline mit 

    -Danyprop=anyval

definiert, System-Properties sind die Umgebungsvariablen des Systems. 

Die Idee dieser Erweiterung besteht darin, Konfigurations-Code in Child-Poms zu sparen und dort nur gewisse Eigenschaften zu definieren, 
die ein Profile aktivieren. Die Konfiguration für ein Profile wird dann einmalig in der Parent-Pom vorgenommen.

Diese Erweiterung sucht nach einem Property-File *profileactivation.properties* im Root-Verzeichnis eines Projektes und stellt alle dort 
definierten Variablen ebenfalls für die Expression-Language Evaluierung zur Verfügung. Die Properties überschreiben sich in der folgenden Reihenfolge:

* Properties aus dem Property-File *profileactivation.properties*
* System Properties
* Userdefined Properties






