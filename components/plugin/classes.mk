CLASSES=\
	org.joval.discovery.Local							\
	org.joval.intf.io.IFile								\
	org.joval.intf.io.IFileEx							\
	org.joval.intf.io.IFileMetadata							\
	org.joval.intf.io.IFilesystem							\
	org.joval.intf.io.IReader							\
	org.joval.intf.io.IReaderGobbler						\
	org.joval.intf.plugin.IAdapter							\
	org.joval.intf.system.IBaseSession						\
	org.joval.intf.system.IEnvironment						\
	org.joval.intf.system.IProcess							\
	org.joval.intf.system.ISession							\
	org.joval.intf.system.ISessionProvider						\
	org.joval.intf.unix.io.IUnixFileInfo						\
	org.joval.intf.unix.io.IUnixFilesystem						\
	org.joval.intf.unix.io.IUnixFilesystemDriver					\
	org.joval.intf.unix.system.IUnixSession						\
	org.joval.intf.util.IConfigurable						\
	org.joval.intf.util.IPathRedirector						\
	org.joval.intf.util.IPerishable							\
	org.joval.intf.util.ISearchable							\
	org.joval.intf.util.tree.IForest						\
	org.joval.intf.util.tree.INode							\
	org.joval.intf.util.tree.ITree							\
	org.joval.intf.windows.identity.IACE						\
	org.joval.intf.windows.identity.IDirectory					\
	org.joval.intf.windows.identity.IGroup						\
	org.joval.intf.windows.identity.IPrincipal					\
	org.joval.intf.windows.identity.IUser						\
	org.joval.intf.windows.io.IWindowsFileInfo					\
	org.joval.intf.windows.io.IWindowsFilesystem					\
	org.joval.intf.windows.registry.IBinaryValue					\
	org.joval.intf.windows.registry.IDwordValue					\
	org.joval.intf.windows.registry.IExpandStringValue				\
	org.joval.intf.windows.registry.IKey						\
	org.joval.intf.windows.registry.ILicenseData					\
	org.joval.intf.windows.registry.IMultiStringValue				\
	org.joval.intf.windows.registry.INoneValue					\
	org.joval.intf.windows.registry.IQwordValue					\
	org.joval.intf.windows.registry.IRegistry					\
	org.joval.intf.windows.registry.IStringValue					\
	org.joval.intf.windows.registry.IValue						\
	org.joval.intf.windows.powershell.IRunspace					\
	org.joval.intf.windows.powershell.IRunspacePool					\
	org.joval.intf.windows.system.IWindowsSession					\
	org.joval.intf.windows.wmi.ISWbemEventSource					\
	org.joval.intf.windows.wmi.ISWbemObject						\
	org.joval.intf.windows.wmi.ISWbemObjectSet					\
	org.joval.intf.windows.wmi.ISWbemProperty					\
	org.joval.intf.windows.wmi.ISWbemPropertySet					\
	org.joval.intf.windows.wmi.IWmiProvider						\
	org.joval.io.BufferedReader							\
	org.joval.io.PerishableReader							\
	org.joval.io.StreamLogger							\
	org.joval.io.fs.CacheFile							\
	org.joval.io.fs.CacheFileSerializer						\
	org.joval.io.fs.CacheFilesystem							\
	org.joval.io.fs.DefaultFile							\
	org.joval.io.fs.FileAccessor							\
	org.joval.io.fs.FileInfo							\
	org.joval.os.unix.io.driver.AIXDriver						\
	org.joval.os.unix.io.driver.LinuxDriver						\
	org.joval.os.unix.io.driver.MacOSXDriver					\
	org.joval.os.unix.io.driver.SolarisDriver					\
	org.joval.os.unix.io.UnixFileInfo						\
	org.joval.os.unix.io.UnixFileSearcher						\
	org.joval.os.unix.io.UnixFilesystem						\
	org.joval.os.unix.macos.DsclTool						\
	org.joval.os.unix.system.BaseUnixSession					\
	org.joval.os.unix.system.Environment						\
	org.joval.os.unix.system.UnixSession						\
	org.joval.os.windows.identity.ActiveDirectory					\
	org.joval.os.windows.identity.Directory						\
	org.joval.os.windows.identity.Group						\
	org.joval.os.windows.identity.LocalACE						\
	org.joval.os.windows.identity.LocalDirectory					\
	org.joval.os.windows.identity.Principal						\
	org.joval.os.windows.identity.User						\
	org.joval.os.windows.io.WindowsFileInfo						\
	org.joval.os.windows.io.WindowsFileSearcher					\
	org.joval.os.windows.io.WindowsFilesystem					\
	org.joval.os.windows.io.WindowsMount						\
	org.joval.os.windows.io.WOW3264FilesystemRedirector				\
	org.joval.os.windows.powershell.PowershellException				\
	org.joval.os.windows.powershell.Runspace					\
	org.joval.os.windows.powershell.RunspacePool					\
	org.joval.os.windows.registry.BaseRegistry					\
	org.joval.os.windows.registry.BinaryValue					\
	org.joval.os.windows.registry.DwordValue					\
	org.joval.os.windows.registry.ExpandStringValue					\
	org.joval.os.windows.registry.Key						\
	org.joval.os.windows.registry.LicenseData					\
	org.joval.os.windows.registry.MultiStringValue					\
	org.joval.os.windows.registry.QwordValue					\
	org.joval.os.windows.registry.Registry						\
	org.joval.os.windows.registry.StringValue					\
	org.joval.os.windows.registry.Value						\
	org.joval.os.windows.registry.WOW3264RegistryRedirector				\
	org.joval.os.windows.system.Environment						\
	org.joval.os.windows.system.WindowsSession					\
	org.joval.os.windows.wmi.WmiException						\
	org.joval.os.windows.wmi.scripting.SWbemObject					\
	org.joval.os.windows.wmi.scripting.SWbemObjectSet				\
	org.joval.os.windows.wmi.scripting.SWbemProperty				\
	org.joval.os.windows.wmi.scripting.SWbemPropertySet				\
	org.joval.os.windows.wmi.WmiProvider						\
	org.joval.os.windows.pe.Characteristics						\
	org.joval.os.windows.pe.DLLCharacteristics					\
	org.joval.os.windows.pe.Header							\
	org.joval.os.windows.pe.ImageDataDirectory					\
	org.joval.os.windows.pe.ImageDOSHeader						\
	org.joval.os.windows.pe.ImageFileHeader						\
	org.joval.os.windows.pe.ImageNTHeaders						\
	org.joval.os.windows.pe.ImageOptionalHeader					\
	org.joval.os.windows.pe.ImageOptionalHeader32					\
	org.joval.os.windows.pe.ImageOptionalHeader64					\
	org.joval.os.windows.pe.ImageSectionHeader					\
	org.joval.os.windows.pe.LanguageConstants					\
	org.joval.os.windows.pe.resource.ImageResourceDataEntry				\
	org.joval.os.windows.pe.resource.ImageResourceDirectory				\
	org.joval.os.windows.pe.resource.ImageResourceDirectoryEntry			\
	org.joval.os.windows.pe.resource.Types						\
	org.joval.os.windows.pe.resource.version.StringFileInfo				\
	org.joval.os.windows.pe.resource.version.StringStructure			\
	org.joval.os.windows.pe.resource.version.StringTable				\
	org.joval.os.windows.pe.resource.version.Var					\
	org.joval.os.windows.pe.resource.version.VarFileInfo				\
	org.joval.os.windows.pe.resource.version.VsFixedFileInfo			\
	org.joval.os.windows.pe.resource.version.VsVersionInfo				\
	org.joval.plugin.LocalPlugin							\
	org.joval.scap.oval.adapter.aix.FilesetAdapter					\
	org.joval.scap.oval.adapter.aix.FixAdapter					\
	org.joval.scap.oval.adapter.aix.OslevelAdapter					\
	org.joval.scap.oval.adapter.independent.BaseFileAdapter				\
	org.joval.scap.oval.adapter.independent.Environmentvariable58Adapter		\
	org.joval.scap.oval.adapter.independent.EnvironmentvariableAdapter		\
	org.joval.scap.oval.adapter.independent.FamilyAdapter				\
	org.joval.scap.oval.adapter.independent.Filehash58Adapter			\
	org.joval.scap.oval.adapter.independent.FilehashAdapter				\
	org.joval.scap.oval.adapter.independent.Textfilecontent54Adapter		\
	org.joval.scap.oval.adapter.independent.TextfilecontentAdapter			\
	org.joval.scap.oval.adapter.independent.XmlfilecontentAdapter			\
	org.joval.scap.oval.adapter.linux.PartitionAdapter				\
	org.joval.scap.oval.adapter.linux.RpminfoAdapter				\
	org.joval.scap.oval.adapter.linux.SelinuxbooleanAdapter				\
	org.joval.scap.oval.adapter.linux.SelinuxsecuritycontextAdapter			\
	org.joval.scap.oval.adapter.macos.AccountinfoAdapter				\
	org.joval.scap.oval.adapter.macos.PlistAdapter					\
	org.joval.scap.oval.adapter.macos.Pwpolicy59Adapter				\
	org.joval.scap.oval.adapter.solaris.IsainfoAdapter				\
	org.joval.scap.oval.adapter.solaris.PackageAdapter				\
	org.joval.scap.oval.adapter.solaris.Patch54Adapter				\
	org.joval.scap.oval.adapter.solaris.PatchAdapter				\
	org.joval.scap.oval.adapter.solaris.SmfAdapter					\
	org.joval.scap.oval.adapter.unix.FileAdapter					\
	org.joval.scap.oval.adapter.unix.InetdAdapter					\
	org.joval.scap.oval.adapter.unix.PasswordAdapter				\
	org.joval.scap.oval.adapter.unix.ProcessAdapter					\
	org.joval.scap.oval.adapter.unix.RunlevelAdapter				\
	org.joval.scap.oval.adapter.unix.ShadowAdapter					\
	org.joval.scap.oval.adapter.unix.UnameAdapter					\
	org.joval.scap.oval.adapter.unix.XinetdAdapter					\
	org.joval.scap.oval.adapter.windows.AccesstokenAdapter				\
	org.joval.scap.oval.adapter.windows.AuditeventpolicyAdapter			\
	org.joval.scap.oval.adapter.windows.AuditeventpolicysubcategoriesAdapter	\
	org.joval.scap.oval.adapter.windows.BaseRegkeyAdapter				\
	org.joval.scap.oval.adapter.windows.CmdletAdapter				\
	org.joval.scap.oval.adapter.windows.FileauditedpermissionsAdapter		\
	org.joval.scap.oval.adapter.windows.FileAdapter					\
	org.joval.scap.oval.adapter.windows.FileeffectiverightsAdapter			\
	org.joval.scap.oval.adapter.windows.GroupAdapter				\
	org.joval.scap.oval.adapter.windows.GroupSidAdapter				\
	org.joval.scap.oval.adapter.windows.LicenseAdapter				\
	org.joval.scap.oval.adapter.windows.LockoutpolicyAdapter			\
	org.joval.scap.oval.adapter.windows.PasswordpolicyAdapter			\
	org.joval.scap.oval.adapter.windows.Process58Adapter				\
	org.joval.scap.oval.adapter.windows.RegistryAdapter				\
	org.joval.scap.oval.adapter.windows.RegkeyeffectiverightsAdapter		\
	org.joval.scap.oval.adapter.windows.SidAdapter					\
	org.joval.scap.oval.adapter.windows.SidSidAdapter				\
	org.joval.scap.oval.adapter.windows.UserAdapter					\
	org.joval.scap.oval.adapter.windows.UserSid55Adapter				\
	org.joval.scap.oval.adapter.windows.UserSidAdapter				\
	org.joval.scap.oval.adapter.windows.Wmi57Adapter				\
	org.joval.scap.oval.adapter.windows.WmiAdapter					\
	org.joval.scap.oval.adapter.windows.WuaupdatesearcherAdapter			\
	org.joval.scap.oval.sysinfo.SysinfoFactory					\
	org.joval.scap.oval.sysinfo.UnixNetworkInterface				\
	org.joval.scap.oval.sysinfo.UnixSystemInfo					\
	org.joval.scap.oval.sysinfo.WindowsSystemInfo					\
	org.joval.test.AD								\
	org.joval.test.Default								\
	org.joval.test.Exec								\
	org.joval.test.FS								\
	org.joval.test.Powershell							\
	org.joval.test.Reg								\
	org.joval.test.WMI								\
	org.joval.util.AbstractBaseSession						\
	org.joval.util.AbstractEnvironment						\
	org.joval.util.AbstractSession							\
	org.joval.util.Base64								\
	org.joval.util.Configurator							\
	org.joval.util.Environment							\
	org.joval.util.PropertyHierarchy						\
	org.joval.util.SafeCLI								\
	org.joval.util.SessionException							\
	org.joval.util.tree.Forest							\
	org.joval.util.tree.Node							\
	org.joval.util.tree.NodeSerializer						\
	org.joval.util.tree.Tree							\
	org.joval.util.tree.TreeHash
