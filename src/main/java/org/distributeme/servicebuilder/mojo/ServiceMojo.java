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
import java.util.List;

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
	private String binDirectoryName = "bin";
	private String libDirectoryName = "lib";
	private String confDirectoryName = "conf";
	private String logDirectoryName = "log";
	private String locallibDirectoryName = "locallib";
	@Parameter
	private String pathToEnvironmentSh;


	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Hello, world." );

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
		JsonArray jsonArr = jo.getAsJsonArray("services");

		Gson gson = new GsonBuilder().create();
		Type listType = new TypeToken<List<ServiceEntry>>() {}.getType();
		ArrayList<ServiceEntry> list = gson.fromJson(jsonArr, listType);
		try{
			generateCommonPart();
		}catch(IOException ex){
			throw new MojoExecutionException("Can't create common directories", ex);
		}

		for (ServiceEntry entry : list){
			try{
				generateService(entry);
			}catch(IOException ex){
				throw new MojoExecutionException("Can't create service "+entry.getName(), ex);
			}
		}


	}

	public File getDefinitionsFile() {
		return definitionsFile;
	}

	public void setDefinitionsFile(File definitionsFile) {
		this.definitionsFile = definitionsFile;
	}

	protected void generateCommonPart() throws IOException{
		String target = "target/"+outputDirectory+"/";

		File bin = new File(target + binDirectoryName); bin.mkdirs();
		File lib = new File(target + libDirectoryName); lib.mkdirs();
		File conf = new File(target + confDirectoryName); conf.mkdirs();

		writeOutGenericScripts(bin);

		//link to environment sh.
		Path envLinkTarget = Paths.get(pathToEnvironmentSh);
		Path envLinksource = Paths.get(target+"environment.sh");
		try {
			Files.createSymbolicLink(envLinksource, envLinkTarget);
		}catch(FileAlreadyExistsException toignore){
			getLog().info("symlink "+envLinksource+" already exists, skipping");
		}


	}

	protected void generateService(ServiceEntry entry) throws IOException{
		String target = "target/"+outputDirectory+"/"+entry.getName()+"/";
		String common = "../";

		File log = new File(target + logDirectoryName); log.mkdirs();
		File localLib = new File(target + locallibDirectoryName); localLib.mkdirs();
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
		serviceDefinitionFile.write(("export TARGET_PID="+serviceEntry.getName()+".pid").getBytes());
		serviceDefinitionFile.write(("export TARGET_CLASS="+serviceEntry.getStartClass()).getBytes());
		//export LOCAL_RMI_PORT=9405
		serviceDefinitionFile.close();

		writeOutScript(targetDirectory, "start_service.sh");
		writeOutScript(targetDirectory, "stop_service.sh");


	}

	protected void writeOutGenericScripts(File targetDirectory)throws IOException{
		writeOutScript(targetDirectory, "start.sh");
		writeOutScript(targetDirectory, "stop.sh");
	}

	protected void writeOutScript(File targetDirectory, String filename) throws IOException{
		InputStream fIn = ServiceMojo.class.getResourceAsStream("/"+filename);
		FileOutputStream fOut = new FileOutputStream(targetDirectory.getAbsolutePath()+"/"+filename);
		int c = -1;
		while ( (c = fIn.read()) != -1 )
			fOut.write(c);
		fOut.close();
	}
}
