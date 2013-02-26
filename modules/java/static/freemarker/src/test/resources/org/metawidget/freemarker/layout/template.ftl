<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<parent>
		<groupId>org.metawidget.modules.static</groupId>
		<artifactId>static-parent</artifactId>
		<version>3.2-SNAPSHOT</version>
		<relativePath>../</relativePath>
	</parent>
	<modelVersion>4.0.0</modelVersion>

	<artifactId>metawidget-static-freemarker</artifactId>
	<packaging>jar</packaging>

	<dependencies>
		<dependency>
			<groupId>org.metawidget.modules.static</groupId>
			<artifactId>metawidget-static</artifactId>
			<version>${project.version}</version>
		</dependency>	
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
		</dependency>
		<dependency>
			<groupId>org.metawidget.modules</groupId>
			<artifactId>metawidget-core</artifactId>
			<type>test-jar</type>
			<scope>test</scope>
		</dependency>	
		<dependency>
			<groupId>org.metawidget.modules</groupId>
			<artifactId>metawidget-annotation</artifactId>
			<version>${project.version}</version>
			<scope>test</scope>
		</dependency>		
	</dependencies>

</project>