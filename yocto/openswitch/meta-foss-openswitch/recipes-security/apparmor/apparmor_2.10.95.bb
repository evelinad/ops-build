SUMMARY = "AppArmor user-space tools"

DESCRIPTION = "AppArmor is MAC style security extension for the Linux kernel."

HOMEPAGE = "http://wiki.apparmor.net/"

SECTION = "base"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM ="file://LICENSE;md5=fd57a4b0bc782d7b80fd431f10bbf9d0"

SRC_URI = "https://launchpad.net/apparmor/2.11/2.11.beta1/+download/apparmor-2.10.95.tar.gz;name=tar \
  file://debugedit.patch \
  file://apparmor_load.sh \
  file://apparmor_unload.sh \
  file://apparmor.service \
"
SRC_URI[tar.md5sum] = "71a13b9d6ae0bca4f5375984df1a51e7"
SRC_URI[tar.sha256sum] = "3f659a599718f4a5e2a33140916715f574a5cb3634a6b9ed6d29f7b0617e4d1a"

DEPENDS = "bison flex"
RDEPENDS_${PN} = "perl bash"

PACKAGES += "${PN}-python"
FILES_${PN}-python = "/usr/lib/python*"

SYSTEMD_SERVICE_${PN} = "apparmor.service"

inherit systemd

B = "${S}"

do_configure() {
  cd libraries/libapparmor/
  ./configure \
      --build=${BUILD_SYS} \
		  --host=${HOST_SYS} \
		  --target=${TARGET_SYS} \
		  --libdir=${libdir} \
		  --mandir=${mandir} \
		  --includedir=${includedir}
}

do_compile() {
  ${MAKE} -C ${S}/libraries/libapparmor
  ${MAKE} -C ${S}/parser apparmor_parser manpages
  ${MAKE} -C ${S}/binutils
  ${MAKE} -C ${S}/utils
  ${MAKE} -C ${S}/profiles
}

do_install() {
  ${MAKE} "DESTDIR=${D}" -C ${S}/libraries/libapparmor install
  ${MAKE} "DESTDIR=${D}" -C ${S}/parser install
  ${MAKE} "DESTDIR=${D}" -C ${S}/binutils install
  ${MAKE} "DESTDIR=${D}" -C ${S}/utils install

  # We do not need the init helpers as we're using systemd
  rm -fr ${D}/lib/
  install -d ${D}${systemd_unitdir}/system
  install -m 644 ${WORKDIR}/*.service ${D}${systemd_unitdir}/system

  #
  # Install profiles
  #
  install -d ${D}/etc/apparmor.d/abstractions
  # Cherry pick profiles we care about
  cd ${S}/profiles/apparmor.d
  cp -Rp abstractions/apparmor_api ${D}/etc/apparmor.d/abstractions
  for i in authentication base bash consoles openssl \
    perl private-files* python ssl_* web-data wutmp
  do
    cp -Rp abstractions/$i ${D}/etc/apparmor.d/abstractions
  done
  # Remove dovecot and XDG things
  rm -f tunables/dovecot
  rm -fr tunables/xdg-*
  sed -i '/xdg/d' tunables/global
  # Install all other tunables
  cp -Rp tunables ${D}/etc/apparmor.d/

  # TODO(bluecmd): These should be shipped with the relevant packages
  for i in bin.* sbin.syslog-ng usr.sbin.ntpd usr.sbin.traceroute
  do
    cp $i ${D}/etc/apparmor.d/
  done
}
