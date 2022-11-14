package org.distributeme.servicebuilder.mojo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.anotheria.util.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 14/02/2017 13:40
 */
@Mojo(name="service")
public class ServiceMojo extends AbstractMojo {

	@Parameter
	private File definitionsFile;

	@Parameter
	private String outputDirectory = "distribution";

	@Parameter
	private String dockerOutputDirectory = "docker";

	@Parameter
	private String binDirectoryName = "bin";
	private String libDirectoryName = "lib";
	private String confDirectoryName = "conf";
	private String logDirectoryName = "logs";
	private String locallibDirectoryName = "locallib";
	private String localconfDirectoryName = "localconf";
	@Parameter
	private String pathToEnvironmentSh;

	private HashMap<String,List<String>> knownProfiles = new HashMap<>();


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Building distributeme-based services." );

		if (definitionsFile == null){
			getLog().info("Definition file is null");
			throw new MojoExecutionException("Can't process definition file, it is not set");
		}

		if (!definitionsFile.exists()){
			getLog().info("Definition file doesn't exists - " + definitionsFile.getAbsolutePath());
			throw new MojoExecutionException("Can't process definition file, it doesn't exists "+definitionsFile.getAbsolutePath());
		}

		byte[] fileData = null;
		try {
			fileData = IOUtils.readFileAtOnce(definitionsFile);
		}catch(IOException e){
			throw new MojoExecutionException("Can't read file "+definitionsFile.getAbsolutePath(), e);
		}

		JsonParser jsonParser = new JsonParser();
		JsonObject jo = (JsonObject)jsonParser.parse(new String(fileData));

		//read profiles
		JsonArray profiles = jo.getAsJsonArray("profiles");
		for (int i=0; i<profiles.size(); i++){
			knownProfiles.put(profiles.get(i).getAsJsonObject().get("name").getAsString(), new LinkedList<String>());
		}


		JsonArray jsonArr = jo.getAsJsonArray("services");

		Gson gson = new GsonBuilder().create();
		Type listType = new TypeToken<List<ServiceEntry>>() {}.getType();
		ArrayList<ServiceEntry> list = gson.fromJson(jsonArr, listType);

		for (ServiceEntry entry : list){
			try{
				generateService(entry);
			}catch(IOException ex){
				throw new MojoExecutionException("Can't create service "+entry.getName(), ex);
			}
		}

		//generate generic start script for all services.
		try{
			generateCommonPart(list);
		}catch(IOException ex){
			throw new MojoExecutionException("Can't create common directories", ex);
		}

		try {
			generateDocker(list);
		}catch(IOException e){
			throw new MojoExecutionException("Can't create docker directories and content", e);
		}

	}

	private void generateDocker(ArrayList<ServiceEntry> services) throws IOException{
		getLog().info("Generating docker files");
		String target = "target/"+dockerOutputDirectory+"/";
		File targetDirFile = new File(target);
		if (!targetDirFile.exists()){
			targetDirFile.mkdirs();
		}

		String containerDir = target+"service/";
		String scriptsDir = target+"scripts/";
		File containerDirFile = new File(containerDir); containerDirFile.mkdirs();
		File scriptsDirFile = new File(scriptsDir); scriptsDirFile.mkdirs();

		writeOutScript(containerDirFile, "docker/start.sh", "start.sh");
		writeOutScript(containerDirFile, "docker/Dockerfile", "Dockerfile");
		writeOutScript(containerDirFile, "docker/.profile", ".profile");
		writeOutScript(containerDirFile, "docker/java.policy", "java.policy");

		File lib = new File(containerDir + libDirectoryName); lib.mkdirs();
		File conf = new File(containerDir + confDirectoryName); conf.mkdirs();

		for (ServiceEntry service : services){
			File envFile = new File(scriptsDir + service.getName() + ".env");
			FileOutputStream fOutEnvFile = new FileOutputStream(envFile);
			fOutEnvFile.write(("SERVICE_CLASS="+service.getStartClass()+"\n").getBytes());
			fOutEnvFile.write(("SERVICE_PORT="+service.getRmiPort()+"\n").getBytes());
			fOutEnvFile.write(("JVM_OPTIONS="+service.getJvmOptions()+"\n").getBytes());
			fOutEnvFile.close();

			File startFile = new File(scriptsDir + service.getName() + ".sh");
			FileOutputStream fOutStartFile = new FileOutputStream(startFile);
			fOutStartFile.write("#!/bin/bash\n".getBytes());
			fOutStartFile.write("source environment.sh\n".getBytes());
			fOutStartFile.write(("docker run --env CONFIGUREME_ENVIRONMENT=$CONFIGUREME_ENVIRONMENT --env SERVICE_REGISTRATION_IP=$SERVICE_REGISTRATION_IP --env-file "+service.getName()+".env -p "+service.getRmiPort()+":"+service.getRmiPort()+
					" tcl-service"//container - name we must configure yet.

				+"\n").getBytes());
			fOutStartFile.close();
			startFile.setExecutable(true);
		}

	}

	public File getDefinitionsFile() {
		return definitionsFile;
	}

	public void setDefinitionsFile(File definitionsFile) {
		this.definitionsFile = definitionsFile;
	}

	protected void generateCommonPart(List<ServiceEntry> entries) throws IOException{
		String target = "target/"+outputDirectory+"/";

		File bin = new File(target + binDirectoryName); bin.mkdirs();
		File lib = new File(target + libDirectoryName); lib.mkdirs();
		File conf = new File(target + confDirectoryName); conf.mkdirs();

		//link to environment sh.
		Path envLinkTarget = Paths.get(pathToEnvironmentSh);
		Path envLinksource = Paths.get(target+"environment.sh");
		try {
			Files.createSymbolicLink(envLinksource, envLinkTarget);
		}catch(FileAlreadyExistsException toignore){
			getLog().info("symlink "+envLinksource+" already exists, skipping");
		}

		writeOutGenericScripts(bin, entries);


	}

	protected void generateService(ServiceEntry entry) throws IOException{

		//add service to the corresponding profile service list.
		List<String> serviceProfiles = entry.getProfiles();
		for (String p : serviceProfiles){
			List<String> servicesForProfile = knownProfiles.get(p);
			if (serviceProfiles == null)
				throw new RuntimeException("Can't find profile '"+p+"' for service '"+entry.getName()+"'");
			servicesForProfile.add(entry.getName());
		}

		String target = "target/"+outputDirectory+"/"+entry.getName()+"/";
		String common = "../";

		File log = new File(target + logDirectoryName); log.mkdirs();
		File localLib = new File(target + locallibDirectoryName); localLib.mkdirs();
		File localConf = new File(target + localconfDirectoryName); localConf.mkdirs();
		File bin = new File(target + binDirectoryName); bin.mkdirs();

		Path libLinkTarget = Paths.get(common+libDirectoryName);
		Path libLinksource = Paths.get(target+libDirectoryName);
		try{
			Files.createSymbolicLink( libLinksource, libLinkTarget);
		}catch(FileAlreadyExistsException toignore){
			getLog().info("symlink "+libLinksource+" already exists, skipping");
		}

		Path confLinkTarget = Paths.get(common+confDirectoryName);
		Path confLinksource = Paths.get(target+confDirectoryName);
		try{
			Files.createSymbolicLink( confLinksource, confLinkTarget);
		}catch(FileAlreadyExistsException toignore){
			getLog().info("symlink "+confLinksource+" already exists, skipping");
		}

		//generate service specific scripts
		generateServiceStartScripts(bin, entry);
	}

	protected void generateServiceStartScripts(File targetDirectory, ServiceEntry serviceEntry) throws IOException{
		//first write out the service definition file.
		FileOutputStream serviceDefinitionFile = new FileOutputStream(targetDirectory+"/service.sh");
		writeLine(serviceDefinitionFile, "#This is service definition file, it is sourced from the start script.");
		writeLine(serviceDefinitionFile,"#This service definition is created from services.json with entry: "+serviceEntry+".");
		writeLine(serviceDefinitionFile,"export SERVICE_NAME="+serviceEntry.getName());
		writeLine(serviceDefinitionFile,"export TARGET_PID="+serviceEntry.getName()+".pid");
		writeLine(serviceDefinitionFile,"export TARGET_CLASS="+serviceEntry.getStartClass());
		writeLine(serviceDefinitionFile,"export RMI_PORT="+serviceEntry.getRmiPort());
		writeLine(serviceDefinitionFile,"export JVM_OPTIONS=\""+(serviceEntry.getJvmOptions()!=null ? serviceEntry.getJvmOptions() : "none")+"\"");
		writeLine(serviceDefinitionFile,"## profiles for this service are: "+serviceEntry.getProfiles());
		//export LOCAL_RMI_PORT=9405
		serviceDefinitionFile.close();

		writeOutScript(targetDirectory, "start_service.sh");
		writeOutScript(targetDirectory, "stop_service.sh");


	}

	protected void writeOutGenericScripts(File targetDirectory, List<ServiceEntry> serviceEntries) throws IOException{
		writeOutScript(targetDirectory, "start.sh");
		writeOutScript(targetDirectory, "stop.sh");

		//create start all script
		String serviceList = "";
		for (ServiceEntry entry : serviceEntries){
			//if (entry.isAutostart()){
			//	serviceList+= entry.getName()+" ";
			//}
		}

		File startAllFile = new File(targetDirectory.getAbsolutePath()+"/"+"start_all.sh");
		FileOutputStream startAll = new FileOutputStream(startAllFile);
		writeLine(startAll, "#!/usr/bin/env bash");
		writeLine(startAll ,"source environment.sh");
		writeLine(startAll ,"echo current profile: $DISTRIBUTEME_PROFILE");
		writeLine(startAll ,"profile_found=\"false\"");

		//generate service definition for every profile.
		for (Map.Entry<String, List<String>> entries : knownProfiles.entrySet()){
			String serviceListForProfile = "";
			for (String s : entries.getValue()){
				serviceListForProfile += s + " ";
			}
			String profileNameForScript = "SERVICES_"+entries.getKey();
			writeLine(startAll, profileNameForScript+"=\""+serviceListForProfile+"\"");
		}
		writeLine(startAll, "");

		//generate profile name check for every profile.
		for (Map.Entry<String, List<String>> entries : knownProfiles.entrySet()){
			String profileNameForScript = "SERVICES_"+entries.getKey();
			writeLine(startAll, "if [ \""+entries.getKey()+"\" = \"$DISTRIBUTEME_PROFILE\" ]; then");
			writeLine(startAll, "  profile_found=\"true\"");
			writeLine(startAll, "  SERVICES=$"+profileNameForScript);
			writeLine(startAll, "fi");
		}
		writeLine(startAll, "");

		writeLine(startAll, "if [ $profile_found = \"false\" ]; then");
		writeLine(startAll, "  echo profile $DISTRIBUTEME_PROFILE not found!");
		writeLine(startAll, "else");
		writeLine(startAll, "  echo starting services $SERVICES");
		writeLine(startAll, "fi");

		writeLine(startAll, "");
		writeLine(startAll, "for i in $SERVICES; do");
		writeLine(startAll, "\techo starting service $i");
		writeLine(startAll, "\tcd $i");
		writeLine(startAll, "\tbin/start_service.sh");
		writeLine(startAll, "\tcd ..");
		writeLine(startAll, "done");
		startAll.close();
		startAllFile.setExecutable(true);

		File stopAllFile = new File(targetDirectory.getAbsolutePath()+"/"+"stop_all.sh");
		FileOutputStream stopAll = new FileOutputStream(stopAllFile);
		writeLine(stopAll,"#!/usr/bin/env bash");
		writeLine(stopAll ,"source environment.sh");
		writeLine(stopAll ,"echo current profile: $DISTRIBUTEME_PROFILE");
		writeLine(stopAll ,"profile_found=\"false\"");

		//generate service definition for every profile.
		for (Map.Entry<String, List<String>> entries : knownProfiles.entrySet()){
			String serviceListForProfile = "";
			for (String s : entries.getValue()){
				serviceListForProfile += s + " ";
			}
			String profileNameForScript = "SERVICES_"+entries.getKey();
			writeLine(stopAll, profileNameForScript+"=\""+serviceListForProfile+"\"");
		}
		writeLine(stopAll, "");

		//generate profile name check for every profile.
		for (Map.Entry<String, List<String>> entries : knownProfiles.entrySet()){
			String profileNameForScript = "SERVICES_"+entries.getKey();
			writeLine(stopAll, "if [ \""+entries.getKey()+"\" = \"$DISTRIBUTEME_PROFILE\" ]; then");
			writeLine(stopAll, "  profile_found=\"true\"");
			writeLine(stopAll, "  SERVICES=$"+profileNameForScript);
			writeLine(stopAll, "fi");
		}
		writeLine(stopAll, "");

		writeLine(stopAll, "if [ $profile_found = \"false\" ]; then");
		writeLine(stopAll, "  echo profile $DISTRIBUTEME_PROFILE not found!");
		writeLine(stopAll, "else");
		writeLine(stopAll, "  echo stoping services $SERVICES");
		writeLine(stopAll, "fi");

		writeLine(stopAll,"for i in $SERVICES; do");
		writeLine(stopAll,"\techo stoping service $i");
		writeLine(stopAll,"\tcd $i");
		writeLine(stopAll,"\tbin/stop_service.sh");
		writeLine(stopAll,"\tcd ..");
		writeLine(stopAll,"done");
		stopAll.close();
		stopAllFile.setExecutable(true);

	}

	private void writeLine(FileOutputStream fOut, String line) throws IOException{
		fOut.write( (line+"\n").getBytes());
	}

	protected void writeOutScript(File targetDirectory, String filename) throws IOException{
		writeOutScript(targetDirectory, filename, filename);
	}

	protected void writeOutScript(File targetDirectory, String filename, String outputFileName) throws IOException{
		InputStream fIn = ServiceMojo.class.getResourceAsStream("/"+filename);
		File file = new File(targetDirectory.getAbsolutePath()+"/"+outputFileName);
		FileOutputStream fOut = new FileOutputStream(file);
		int c = -1;
		while ( (c = fIn.read()) != -1 )
			fOut.write(c);
		fOut.close();
		file.setExecutable(true);
	}
}
