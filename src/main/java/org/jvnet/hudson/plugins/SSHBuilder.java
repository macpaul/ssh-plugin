package org.jvnet.hudson.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class SSHBuilder extends Builder {

	private String siteName;
	private String command;
	private String runtime_cmd;

	@DataBoundConstructor
	public SSHBuilder(String siteName, String command) {
		this.siteName = siteName;
		this.command = command;
	}

	public String getSiteName() {
		return siteName;
	}

	public String getCommand() {
		return command;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		SSHSite site = getSite();

		runtime_cmd = new String(command);
                EnvVars envVars = build.getEnvironment(listener);
                // on Windows environment variables are converted to all upper case,
                // but no such conversions are done on Unix, so to make this cross-platform,
                // convert variables to all upper cases.
                for(Map.Entry<String,String> e : build.getBuildVariables().entrySet()) {
                    envVars.put(e.getKey(),e.getValue());
		    runtime_cmd = runtime_cmd.replace(e.getKey(), e.getValue());
		}

		if (site != null && command != null && command.trim().length() > 0) {
			listener.getLogger().printf("executing script:%n%s%n", runtime_cmd);
			return site.executeCommand(listener.getLogger(), runtime_cmd) == 0;
		}
		return true;
	}

	public SSHSite getSite() {
		SSHSite[] sites = SSHBuildWrapper.DESCRIPTOR.getSites();
		for (SSHSite site : sites) {
			if (site.getName().equals(siteName))
				return site;
		}
		return null;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.SSH_DisplayName();
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			return req.bindJSON(clazz, formData);
		}

		public ListBoxModel doFillSiteNameItems() {
			ListBoxModel m = new ListBoxModel();
			for (SSHSite site : SSHBuildWrapper.DESCRIPTOR.getSites()) {
				m.add(site.getName());
			}
			return m;
		}
	}
}
