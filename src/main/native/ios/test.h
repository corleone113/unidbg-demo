#include <stdlib.h>
#include <stdio.h>
#include <dlfcn.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <errno.h>
#include <sys/stat.h>
#include <sys/sysctl.h>
#include <sys/proc.h>
#include <mach/mach_time.h>

static void test_printf() {
  char buf[0x40];
  memset(buf, 0, 0x40);
  snprintf(buf, 0x40, "snprintf: %p\n", buf);
  fprintf(stderr, "printf[%p] test: %s", buf, buf);
  fprintf(stdout, "ENOTDIR=0x%x, O_WRONLY=0x%x, O_RDWR=0x%x, O_NONBLOCK=0x%x, O_APPEND=0x%x, O_CREAT=0x%x, O_DIRECTORY=0x%x\n", ENOTDIR, O_WRONLY, O_RDWR, O_NONBLOCK, O_APPEND, O_CREAT, O_DIRECTORY);
}

static void test_sysctl_KERN_USRSTACK() {
  int mib[2];
  void *stack = NULL;
  size_t size = sizeof(stack);

  mib[0] = CTL_KERN;
  mib[1] = KERN_USRSTACK;
  int ret = sysctl(mib, 2, &stack, &size, NULL, 0);
  printf("sysctl_KERN_USRSTACK ret=%d, stack=%p, mib=%p, offset=0x%lx\n", ret, stack, mib, ((long) stack - (long) mib));
}

static void test_sysctl_KERN_PROC() {
  int mib[4];
  struct kinfo_proc info;
  size_t size = sizeof(struct kinfo_proc);

  pid_t pid = getpid();
  mib[0] = CTL_KERN;
  mib[1] = KERN_PROC;
  mib[2] = KERN_PROC_PID;
  mib[3] = pid;

  int ret = sysctl(mib, 4, &info, &size, 0, 0);

  struct kinfo_proc *p_info = &info;
  printf("sysctl_KERN_PROC ret=%d, pid=%d, p_realtimer=0x%lx, e_spare=0x%lx\n", ret, pid, ((long) &p_info->kp_proc.p_realtimer - (long) p_info), ((long) &p_info->kp_eproc.e_spare - (long) &p_info->kp_eproc));
}

#define PROC_PIDT_SHORTBSDINFO		13
#define PROC_PIDT_SHORTBSDINFO_SIZE	(sizeof(struct proc_bsdshortinfo))

struct proc_bsdshortinfo {
        uint32_t                pbsi_pid;		/* process id */
        uint32_t                pbsi_ppid;		/* process parent id */
        uint32_t                pbsi_pgid;		/* process perp id */
	uint32_t                pbsi_status;		/* p_stat value, SZOMB, SRUN, etc */
	char                    pbsi_comm[MAXCOMLEN];	/* upto 16 characters of process name */
	uint32_t                pbsi_flags;              /* 64bit; emulated etc */
        uid_t                   pbsi_uid;		/* current uid on process */
        gid_t                   pbsi_gid;		/* current gid on process */
        uid_t                   pbsi_ruid;		/* current ruid on process */
        gid_t                   pbsi_rgid;		/* current tgid on process */
        uid_t                   pbsi_svuid;		/* current svuid on process */
        gid_t                   pbsi_svgid;		/* current svgid on process */
        uint32_t                pbsi_rfu;		/* reserved for future use*/
};

extern int proc_pidinfo(int pid, int flavor, uint64_t arg,  void *buffer, int buffersize);

static void test_proc_pidinfo() {
  struct proc_bsdshortinfo bsdinfo;

  pid_t pid = getpid();
  int ret = proc_pidinfo(pid, PROC_PIDT_SHORTBSDINFO, 1, &bsdinfo, sizeof(bsdinfo));
  printf("proc_pidinfo ret=%d, pid=%d, size=%lu, pbsi_comm=%s, pbsi_flags=0x%x, pbsi_ppid=%d\n", ret, pid, sizeof(bsdinfo), bsdinfo.pbsi_comm, bsdinfo.pbsi_flags, bsdinfo.pbsi_ppid);
}

static void test_pthread() {
  char name[64];
  sprintf(name, "thread: %p", name);
  pthread_setname_np(name);
  memset(name, 0, 64);
  pthread_t thread = pthread_self();
  pthread_getname_np(thread, name, sizeof(name));
  printf("pthread[%p] name=%s\n", thread, name);
}

static void test_file() {
  const char *file = "/tmp/test_file.txt";
  int fd = open(file, O_RDWR | O_CREAT);
  if(fd == -1) {
    printf("open file errno=%d, msg=%s\n", errno, strerror(errno));
  } else {
    close(fd);
    FILE *fp = fopen(file, "r");
    fseek(fp, 0, SEEK_END);
    struct stat statbuf;
    stat(file, &statbuf);
    char buf[256];
    sprintf(buf, "open file success fd=%d, fp=%p, seek_size=%ld, stat_size=%lld\n", fd, fp, ftell(fp), statbuf.st_size);
    fprintf(stdout, "%s", buf);
    FILE *wfp = fopen(file, "a");
    fwrite(buf, 1, strlen(buf), wfp);
    fclose(wfp);
    fclose(fp);
  }
}

static void test_time() {
  struct mach_timebase_info info;
  int ret = mach_timebase_info(&info);
  uint64_t start = mach_absolute_time();
  printf("time ret=%d, numer=%u, denom=%u, start=%llu\n", ret, info.numer, info.denom, start);
  uint64_t end = mach_absolute_time();
  uint64_t elapsed = end - start;
  uint64_t elapsedNanoSeconds = elapsed * info.numer / info.denom;
  printf("time end=%llu, elapsed=%llu, elapsedNanoSeconds=%lluns\n", end, elapsed, elapsedNanoSeconds);
}

void do_test() {
  test_printf();
  test_sysctl_KERN_USRSTACK();
  test_sysctl_KERN_PROC();
  test_proc_pidinfo();
  test_pthread();
  test_file();
  test_time();
}
