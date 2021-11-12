 
Simple run application

Use Gradle version 4.1 https://gradle.org/releases/   

1.Download jar "java-transfer-multithreading.jar" from root directory of github project.     
2.Open "Command Line" and go to directory where "java-transfer-multithreading.jar" located.    
3.Execute in "Command Line" following command: "java -jar java-transfer-multithreading.jar".    
4.Open any browser and put following commands:     

    view all accounts: http://localhost:18080/v1/accounts/all 
    create account with name "1": http://localhost:18080/v1/accounts/createAccount/1 
    create account with name "2": http://localhost:18080/v1/accounts/createAccount/2 
    add balance to account "1" : http://localhost:18080/v1/accounts/1/20/balance/add 
    add balance to account "2" : http://localhost:18080/v1/accounts/2/20/balance/add 
    make transfer from account "1" to "account "2" 
    for amount "20": http://localhost:18080/v1/transfers/process/1/2/20 
    clear all accounts if exists: http://localhost:18080/v1/accounts/clear 
    withdraw balance from account "2": http://localhost:18080/v1/accounts/2/10/balance/withdraw  

5.For exit applicatio just close "Command Line" 
 
 
