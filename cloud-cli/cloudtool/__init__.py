'''
Created on Aug 2, 2010

@author: rudd-o
'''

import sys
import cloudapis as apis
import cloudtool.utils as utils

    
def main(argv=None):
    
    #import ipdb; ipdb.set_trace()
    if argv == None:
       argv = sys.argv

    prelim_args = [ x for x in argv[0:] if not x.startswith('-') ]
    parser = utils.get_parser()
     
    api = __import__("cloudapis")
    apis = getattr(api, "implementor")
    if len(prelim_args) == 1:
        commandlist = utils.get_command_list(apis)
        parser.error("you need to specify a command name as the first argument\n\nCommands supported by the %s API:\n"%prelim_args[0] + "\n".join(commandlist))

    command = utils.lookup_command_in_api(apis,prelim_args[1])
    if not command: parser.error("command %r not supported by the %s API"%(prelim_args[1],prelim_args[0]))
   
    argv = argv[1:] 
    if len(argv) == 1:
	argv.append("--help")

    parser = utils.get_parser(apis.__init__,command)
    opts,args,api_optionsdict,cmd_optionsdict = parser.parse_args(argv)
    
    
    try:
        api = apis(**api_optionsdict)
    except utils.OptParseError,e:
        parser.error(str(e))
    
    command = utils.lookup_command_in_api(api,args[0])

    # we now discard the first two arguments as those necessarily are the api and command names
    args = args[2:]

    try: return command(*args,**cmd_optionsdict)
    except TypeError,e: parser.error(str(e))


if __name__ == '__main__':
    main(argv)
