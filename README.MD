**Java SpringBoot Hydra  Flow: Login->Consent->Code->Token**

Don't forget to change IPAddress(1).IPAddress(2),IPAddress(3),IPAddress(4)

STEP-1: Update HydraDemoAppplication  192.168.45.81 Change to U IPAddress(1).

STEP-2: start HydraDemoApplication 

STEP-3：DOWNLOAD-HYDRA-SOURCE git clone https://github.com/ory/hydra.git 

STEP-4：Update quickstart.yml  
        - URLS_CONSENT=https://IPAddress(2)/consent
        - URLS_LOGIN=https://IPAddress(3)/login
STEP-5：docker-compose -f .\quickstart.yml -f .\quickstart-mysql.yml up --build 

STEP-6: docker-compose -f .\quickstart.yml exec hydra hydra clients create --endpoint http://127.0.0.1:4445 --id java-client --secret mySecret --grant-types authorization_code,refresh_token --response-types code,id_token --scope openid,offline --callbacks https://IPAddress(4)/callback

