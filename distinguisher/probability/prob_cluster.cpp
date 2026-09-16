#include<iostream>
#include<cmath>
using namespace std;

int sbox_4[16] = {12,6,9,0,1,10,2,11,3,8,5,13,4,14,7,15};
int sbox_8[256] = {0x65 , 0x4c , 0x6a , 0x42 , 0x4b , 0x63 , 0x43 , 0x6b , 0x55 , 0x75 , 0x5a , 0x7a , 0x53 , 0x73 , 0x5b , 0x7b ,0x35 , 0x8c , 0x3a , 0x81 , 0x89 , 0x33 , 0x80 , 0x3b , 0x95 , 0x25 , 0x98 , 0x2a , 0x90 , 0x23 , 0x99 , 0x2b ,0xe5 , 0xcc , 0xe8 , 0xc1 , 0xc9 , 0xe0 , 0xc0 , 0xe9 , 0xd5 , 0xf5 , 0xd8 , 0xf8 , 0xd0 , 0xf0 , 0xd9 , 0xf9 ,0xa5 , 0x1c , 0xa8 , 0x12 , 0x1b , 0xa0 , 0x13 , 0xa9 , 0x05 , 0xb5 , 0x0a , 0xb8 , 0x03 , 0xb0 , 0x0b , 0xb9 ,0x32 , 0x88 , 0x3c , 0x85 , 0x8d , 0x34 , 0x84 , 0x3d , 0x91 , 0x22 , 0x9c , 0x2c , 0x94 , 0x24 , 0x9d , 0x2d ,0x62 , 0x4a , 0x6c , 0x45 , 0x4d , 0x64 , 0x44 , 0x6d , 0x52 , 0x72 , 0x5c , 0x7c , 0x54 , 0x74 , 0x5d , 0x7d ,0xa1 , 0x1a , 0xac , 0x15 , 0x1d , 0xa4 , 0x14 , 0xad , 0x02 , 0xb1 , 0x0c , 0xbc , 0x04 , 0xb4 , 0x0d , 0xbd ,0xe1 , 0xc8 , 0xec , 0xc5 , 0xcd , 0xe4 , 0xc4 , 0xed , 0xd1 , 0xf1 , 0xdc , 0xfc , 0xd4 , 0xf4 , 0xdd , 0xfd ,0x36 , 0x8e , 0x38 , 0x82 , 0x8b , 0x30 , 0x83 , 0x39 , 0x96 , 0x26 , 0x9a , 0x28 , 0x93 , 0x20 , 0x9b , 0x29 ,0x66 , 0x4e , 0x68 , 0x41 , 0x49 , 0x60 , 0x40 , 0x69 , 0x56 , 0x76 , 0x58 , 0x78 , 0x50 , 0x70 , 0x59 , 0x79 ,0xa6 , 0x1e , 0xaa , 0x11 , 0x19 , 0xa3 , 0x10 , 0xab , 0x06 , 0xb6 , 0x08 , 0xba , 0x00 , 0xb3 , 0x09 , 0xbb ,0xe6 , 0xce , 0xea , 0xc2 , 0xcb , 0xe3 , 0xc3 , 0xeb , 0xd6 , 0xf6 , 0xda , 0xfa , 0xd3 , 0xf3 , 0xdb , 0xfb ,0x31 , 0x8a , 0x3e , 0x86 , 0x8f , 0x37 , 0x87 , 0x3f , 0x92 , 0x21 , 0x9e , 0x2e , 0x97 , 0x27 , 0x9f , 0x2f ,0x61 , 0x48 , 0x6e , 0x46 , 0x4f , 0x67 , 0x47 , 0x6f , 0x51 , 0x71 , 0x5e , 0x7e , 0x57 , 0x77 , 0x5f , 0x7f ,0xa2 , 0x18 , 0xae , 0x16 , 0x1f , 0xa7 , 0x17 , 0xaf , 0x01 , 0xb2 , 0x0e , 0xbe , 0x07 , 0xb7 , 0x0f , 0xbf ,0xe2 , 0xca , 0xee , 0xc6 , 0xcf , 0xe7 , 0xc7 , 0xef , 0xd2 , 0xf2 , 0xde , 0xfe , 0xd7 , 0xf7 , 0xdf , 0xff};

int ddt4[16][16]={0};
int ddt8[256][256]={0};
int TWEAKEY_P[16] = {9,15,8,13,10,14,12,11,0,1,2,3,4,5,6,7};
void DDT4()
{
   int in, out, m0;
   for (in=0; in<16; in++)
   {
       for (m0=0; m0<16; m0++)
       {
           out = sbox_4[m0] ^ sbox_4[m0^in];
           ddt4[in][out] ++;
       }
   }
}
void DDT8()
{
   int in, out, m0;
   for (in=0; in<256; in++)
   {
       for (m0=0; m0<256; m0++)
       {
           out = sbox_8[m0] ^ sbox_8[m0^in];
           ddt8[in][out] ++;
       }
   }
}


void skinny_64_128()
{
    long double prob=0;
    long double clprob=0;
	  
	for(int u=1; u<16; u++){
		if(ddt4[2][u]>0 && ddt4[u^3][12]>0){
			for(int v=1; v<16; v++){
				if(ddt4[2][v]>0 && ddt4[v][12]>0){
					prob = ddt4[4][2];
					prob *= ddt4[2][u] * ddt4[2][v];
					prob *= ddt4[u^3][12] * ddt4[v][12];
					prob *= ddt4[12][2];
					prob *= ddt4[8][4];
					
					clprob += prob*prob;
				
				}
			}
		}
	}
 
    cout << "The probability of (R0-R7)-round for 18-round skinny_64_128:"<<endl;
    cout << "p="<<log(sqrt(clprob))/log(2.0)-7*4 << endl;
}

/*
void skinny_64_192()
{
    long double prob=0;
    long double clprob=0;
            
    for(int u=1; u<16; u++) {
        if(ddt4[8][u]>0){
        	for(int v=1; v<16; v++){
        		if(ddt4[u][v]>0 && ddt4[v][5]>0){
					prob = ddt4[8][4];
					prob *= ddt4[8][u] * ddt4[8][u] * ddt4[8][u];
					prob *= ddt4[u][v] * ddt4[v][5];
			
					clprob += prob*prob;
				}
			}
    	}
    }
    
    cout << "The probability of (R0-R8)-round for 22-round skinny_64_192:"<<endl;
    cout << "p="<<log(sqrt(clprob))/log(2.0)-6*4 << endl;
    
    prob=0;
    clprob=0;
            
	for(int w=1; w<16; w++){
		if(ddt4[10][w]>0 && ddt4[w][1]>0){
			prob = ddt4[10][w];
			prob *= ddt4[w][1];
	
			clprob += prob*prob;
		}
	}
    
    cout << "The probability of (R14-R21)-round for 22-round skinny_64_192:"<<endl;
    cout << "q="<<log(sqrt(clprob))/log(2.0)-2*4 << endl;
    
}
*/

void skinny_128_256()
{
    long double prob=0;
    long double clprob=0;
            
    for(int u=1; u<256; u++) {
        if(ddt8[32][u]>0 && ddt8[u][3]){
    		prob = ddt8[32][u];
    		prob *= ddt8[u][3];
    		
    		clprob += prob*prob;
    	}
    }
    
    cout << "The probability of (R0-R5)-round for 19-round skinny_128_256:"<<endl;
    cout << "p="<<log(sqrt(clprob))/log(2.0)-2*8 << endl;
    
    prob=0;
    clprob=0;
    
    
    for(int v=1; v<256; v++) {
        if(ddt8[v][221]>0){
            for(int w=1; w<256; w++) {
                if(ddt8[w][v]>0) {
                    for(int w1=1; w1<256; w1++) {
                        if(ddt8[w1][w]>0 && ddt8[w1][179]>0 && ddt8[4][w1]>0) {
                            for(int w2=1; w2<256; w2++) {
                                if(ddt8[w2][w]>0 && ddt8[4][w2]>0) {
                                    for(int w3=1; w3<256; w3++) {
                                        if(ddt8[w3][w]>0 && ddt8[4][w3]>0) {
                                        	prob = ddt8[4][w1] * ddt8[4][w1];
											prob *= ddt8[4][w2];
											prob *= ddt8[4][w3]  * ddt8[4][w3] * ddt8[4][w3];
                                        	prob *= ddt8[w1][179];
											prob *= ddt8[w1][w] * ddt8[w2][w] * ddt8[w3][w];
                                        	prob *= ddt8[w][v];
                                        	prob *= ddt8[v][221];                              
                            
                                   			clprob += prob*prob;
                                        }
                                    }
                                
                                }
                            }
                        }
                    }
                }
            }
        }
    }
 
    cout << "The probability of (R11-R18)-round for 19-round skinny_128_256:"<<endl;
    cout << "q="<<log(sqrt(clprob))/log(2.0)-12*8 << endl;
}

void skinny_128_384()
{
	long double prob=0;
    long double clprob=0;
	
    for(int u=1; u<256; u++) {
    	if(ddt8[12][u]>0 && ddt8[u][118]>0){
    		for(int v=1; v<256; v++){
    			if(ddt8[118][v]>0 && ddt8[v][32]>0){
    				prob = ddt8[12][u] * ddt8[u][118];
        			prob *= ddt8[118][v];
					prob *= ddt8[v][32] * ddt8[v][32] * ddt8[v][32];	
        	
					clprob += prob*prob;
				}
			} 
        }
    }
     
    
    cout << "The probability of (R0-R9)-round for 23-round skinny_128_384:"<<endl;
    cout << "p="<<log(sqrt(clprob))/log(2.0)-6*8 << endl;
    
    prob=0;
    clprob=0;

    for(int w=1; w<256; w++){
    	if(ddt8[w][87] > 0){
    		for(int w0=1; w0<256; w0++){
    			if(ddt8[w0][w]>0 && ddt8[16][w0]>0 && ddt8[25][w0]>0){
    				
					prob = ddt8[16][210];
					prob *= ddt8[16][w0] * ddt8[16][w0] * ddt8[25][w0];
					prob *= ddt8[w0][w] * ddt8[w][87];
			
					clprob += prob*prob;
				}
			}
		}
	}
    
    cout << "The probability of (R14-R22)-round for 23-round skinny_128_384:"<<endl;
    cout << "q="<<log(sqrt(clprob))/log(2.0)-6*8 << endl;

}


int main()
{
    DDT4();
    DDT8();
    skinny_64_128(); 
//    skinny_64_192();
    skinny_128_256();
    skinny_128_384();
    return 0;
}
