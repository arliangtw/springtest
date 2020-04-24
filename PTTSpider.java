package info.diywork.PTTSpider;

import static info.diywork.PTTSpider.Log.console;
import static info.diywork.PTTSpider.Log.fail;
import static info.diywork.PTTSpider.Log.msg;
import info.diywork.PTTSpider.PttMenu.PttStatus;
import info.diywork.PTTSpider.chain.command.ContextParmName;

import java.util.HashMap;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.CatalogFactory;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.chain.impl.ContextBase;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"info.diywork.elasticsearch","info.diywork.PTTSpider"})

public class PTTSpider {
	//設定檔
	private static final String CONFIG_FILE = "command-config.xml";
	public enum FlowName { GetUserList , GetIPList , GetAllPost};
	private static FlowName catalogName;
	private Context ctx = new ContextBase(new HashMap<ContextParmName,String>());
	
	//@SuppressWarnings("resource")
	public static void main(String [] args) throws InterruptedException
	{
		if (args == null || args.length < 1) {
			console("please give me an parameter ");
			console( StringUtils.join(FlowName.values(), " or "));
			return;
		}
		if (args[0].equals(FlowName.GetUserList.name())) {
			catalogName = FlowName.GetUserList;
		}else if (args[0].equals(FlowName.GetIPList.name())) {
			catalogName = FlowName.GetIPList;
		}else if (args[0].equals(FlowName.GetAllPost.name())) {
			catalogName = FlowName.GetAllPost;
		}else{
			console("fail parameter ");
			return;
		}
		System.getProperty("logback.debug", "true");
		new PTTSpider().go();
	}	

	/**
	 * @throws InterruptedException 
	 * @Description 
	 */
	@SuppressWarnings("unchecked")
	public void go() {
		new Log(catalogName.name());
		msg("PTTSpder Go");
		AnnotationConfigApplicationContext springContext = new AnnotationConfigApplicationContext();
		springContext.register(PTTSpider.class);
		springContext.refresh();
		ctx.put(ContextParmName.springApplicationContext, springContext);
		while (true) {
			//不斷執行，永不終止
			try {
				executeChain();
			}catch(Exception e){
				break;
			}
		}
		springContext.close();
		msg("PTTSpder Bye ~");
	}
	
	@SuppressWarnings("unchecked")
	private void executeChain() throws Exception {
		msg("load Catalog");

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		if (classLoader == null) {
		    classLoader = Class.class.getClassLoader();
		}
		
        Digester digester = new ConfigParser().getDigester();
        CatalogFactory catalogFactory = CatalogFactoryBase.getInstance();
        digester.clear();
       	msg("Catalog File in " + classLoader.getResource(CONFIG_FILE).getPath());
		catalogFactory.setCatalog((Catalog) digester.parse(classLoader.getResourceAsStream(CONFIG_FILE)));
        
        msg("executeChain");
        ctx.put(ContextParmName.flowName, catalogName.name());
        
        if (catalogName.equals(FlowName.GetIPList) || catalogName.equals(FlowName.GetUserList)){
        	Command intoPTT = catalogFactory.getCatalog().getCommand("IntoPTT");
        	
    		if (!intoPTT.execute(ctx) && ctx.get(ContextParmName.pttStatus).equals(PttStatus.MAIN_MENU)) {
    			Command catalog = catalogFactory.getCatalog().getCommand(catalogName.name());
    			
    			while(true){
    				if (catalog.execute(ctx)){
    					fail(catalogName + " CommandChain 中斷流程，執行完畢");
    				}else{
    					Log.important(catalogName + " CommandChain 完整流程，執行完畢");
    				}
    				break;
    			}
    		}        	
        	
        }else{
        	Command getAllPost = catalogFactory.getCatalog().getCommand(catalogName.name());
        	ctx.put(ContextParmName.catalogFactory, catalogFactory);
        	while (true) {
        		if (getAllPost.execute(ctx) ) { // && ctx.getPttStatus().equals(PttStatus.MAIN_MENU)
        			fail(catalogName + " CommandChain 中斷流程，執行完畢");
        		}else{
        			Log.important(catalogName + " CommandChain 完整流程，執行完畢");
        		}
        		break;
        	}
        }

	}
		
}
