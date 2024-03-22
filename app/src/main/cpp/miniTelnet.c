#include <stdio.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netdb.h>
#include <string.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <time.h>
#include <stdarg.h>
#include "com_netease_LDNetDiagnoService_LDNetSocket.h"
#include "Provider.h"
#define OUT_LEN 1000

JavaVM *gTelnetJvm = NULL;
pthread_mutex_t mutexTel = PTHREAD_MUTEX_INITIALIZER;
int isFirstTelnet;

JNIEXPORT void JNICALL Java_com_netease_LDNetDiagnoService_LDNetSocket_startJNITelnet(JNIEnv *env, jobject obj, jstring command, jstring port){
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "startJNITelnet begin...." );
	(*env)->GetJavaVM(env, &gTelnetJvm);
	(*gTelnetJvm)->AttachCurrentThread(gTelnetJvm, &env, NULL);

	isFirstTelnet = 1;
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "startJNITelnet c_command begin...." );
	char* c_command = j2c(env, command);
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "startJNITelnet c_command end...." );
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "startJNITelnet c_port begin...." );
	char* c_port = j2c(env, port);
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "startJNITelnet c_port end...." );
	char* argv[]={"connect", c_command, c_port};
	mainConnect(3, argv);
}

int Lprintf(const char *fmt, ...){
	va_list argptr;
	int cnt;
	va_start(argptr, fmt);
	char *buffer = (char *)malloc(OUT_LEN);
	memset(buffer, OUT_LEN, 0);
	cnt = vsnprintf(buffer, OUT_LEN, fmt, argptr);
	buffer[cnt] = '\0';
	pthread_mutex_lock(&mutexTel);
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "print lock:>>>>>>%d", mutexTel.__private[0]);
	PrintSocketInfo(buffer);
	pthread_mutex_unlock(&mutexTel);
	__android_log_print(ANDROID_LOG_INFO, "JNIMsg", "print unlock>>>>>>%d", mutexTel.__private[0]);
	free(buffer);
	va_end(argptr);
	isFirstTelnet++;
	return 1;
}

void connectHost(struct sockaddr_in serv_addr)
{
	int sockfd;
	time_t begin, end;
	double avg =0;
	double ttime[4] ={0};
	int iter;

	for(iter = 0; iter < 4; iter++)
	{
		sockfd = socket(AF_INET, SOCK_STREAM, 0);
		if (sockfd < 0)
		{
			Lprintf("connect to host failed. ERROR opening socket\n");
			return;
		}

		begin = clock();
		if (connect(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0)
		{
			Lprintf("connect to host failed\n");
			return;
		}
		end = clock();
		ttime[iter]=(double)(end-begin)/CLOCKS_PER_SEC * 1000;
		avg+=ttime[iter];
		close(sockfd);
	}
	avg /= 4;
	Lprintf("connect to host %s\n", inet_ntoa(serv_addr.sin_addr));
	Lprintf("1's time:%.0fms, 2's time:%.0fms, 3'time:%.0fms, 4's time:%.0fms, avg time:%.0fms\n",ttime[0],ttime[1],ttime[2],ttime[3],avg);
}

int mainConnect (int argc, char* argv[])
{
	int portno;
	struct hostent *server;
	struct sockaddr_in serv_addr;

	if (argc < 3)
	{
		Lprintf("connect to host failed, argument mismatch\n");
		return 0;
	}

	bzero((char *) &serv_addr, sizeof(serv_addr));
	portno = atoi(argv[2]);
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(portno);
	server = gethostbyname(argv[1]);

	if (server == NULL)
	{
		Lprintf("\n connect to host failed, gethostbyname return null \n");
		return 0;
	}

	int iter;
	for(iter = 0; server->h_addr_list[iter]; iter++)
	{
		char* maddr = inet_ntoa(*(struct in_addr*)(server->h_addr_list[iter]));
		serv_addr.sin_addr.s_addr = inet_addr(maddr);
		connectHost(serv_addr);
	}

	return 0;
}
