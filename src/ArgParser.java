// parsing commandline args in java, (options, similar to argparse for python)
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

import java.util.List;

/**
 * Created by kaiyin on 05/11/14.
 */
public class ArgParser {
    @Parameter
    public List<String> parameters = Lists.newArrayList();
    @Parameter(names = "-file", description = "_haps.gz file to convert")
    public String file;
    @Parameter(names="-outDir", description = "Output folder")
    public String outDirString;
    @Parameter(names="-debug", description = "Debug mode")
    public boolean debug = false;
}

