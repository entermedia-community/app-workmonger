importPackage( Packages.com.openedit.util );
importPackage( Packages.java.util );
importPackage( Packages.java.lang );
importPackage( Packages.java.io );
importPackage( Packages.com.openedit.modules.update );
importPackage( Packages.com.openedit.modules.scheduler );

var war = "http://dev.entermediasoftware.com/projects/workmonger/ROOT.war";

var root = moduleManager.getBean("root").getAbsolutePath();
var web = root + "/WEB-INF";
var tmp = web + "/tmp";

log.add("1. GET THE LATEST WAR FILE");
var downloader = new Downloader();
downloader.download( war, tmp + "/ROOT.war");

log.add("2. UNZIP WAR FILE");
var unziper = new ZipUtil();
unziper.unzip(  tmp + "/ROOT.war",  tmp );

log.add("3. REPLACE LIBS");
var files = new FileUtils();
files.deleteMatch( web + "/lib/workmonger*.jar");
files.deleteMatch( web + "/lib/openedit-tracker*.jar");
files.copyFileByMatch( tmp + "/WEB-INF/lib/workmonger*.jar", web + "/lib/");

log.add("4. UPGRADE BASE DIR");
files.deleteAll( root + "/base/tracker");
files.deleteAll( root + "/WEB-INF/base/tracker");
files.copyFiles( tmp + "/WEB-INF/base/tracker", root + "/WEB-INF/base/tracker");
//files.deleteAll( root + "/WEB-INF/base/themes/devcom");
//files.copyFiles( tmp + "/WEB-INF/base/themes/devcom", root + "/WEB-INF/base/themes/devcom");

log.add("6. CLEAN UP");
files.deleteAll(tmp);

log.add("7. UPGRADE COMPLETED");
log.add("Use the OpenEdit Account Manager to enable permissions on the Tracker Interface");