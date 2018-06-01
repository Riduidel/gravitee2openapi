package org.ndx.gravitee.openapi.main;

import java.io.File;

import com.beust.jcommander.Parameter;

public class Options {
	@Parameter(names= {"-i", "--input"})
	public File input;
	
	@Parameter(names= {"-t", "--transform"})
	public File transform;
	
	@Parameter(names= {"-o", "--output"})
	public File output;
}