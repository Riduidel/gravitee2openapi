package org.ndx.gravitee.openapi.main;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.beust.jcommander.JCommander;

public class Main {
	public static void main(String[] args) throws IOException {
		Options options = new Options();
		JCommander.newBuilder().addObject(options).build().parse(args);
		new Main().run(options);
	}

	/**
	 * Run transformation of input file into output file
	 * @param options
	 * @throws IOException 
	 */
	private void run(Options options) throws IOException {

        // How to access the test artifacts, i.e. JSON files
        //  JsonUtils.classpathToList : assumes you put the test artifacts in your class path
        //  JsonUtils.filepathToList : you can use an absolute path to specify the files

        List chainrSpecJSON = null;
        if(options.transform==null) {
        	chainrSpecJSON = JsonUtils.classpathToList( "/json/gravitee2openapi.json" );
        } else {
        	chainrSpecJSON = JsonUtils.filepathToList(options.transform.getAbsolutePath());
        }
        Chainr chainr = Chainr.fromSpec( chainrSpecJSON );

        Object inputJSON = JsonUtils.filepathToObject(options.input.getAbsolutePath());

        Object transformedOutput = chainr.transform( inputJSON );
        
        options.output.getAbsoluteFile().getParentFile().mkdirs();
        FileUtils.write(options.output, JsonUtils.toPrettyJsonString( transformedOutput ) );
	}
}
