/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
function errMsg()
{
    if (document.all.item("ErrorMessageLabel").innerText !== "") {
            var strMSG = document.all.item("ErrorMessageLabel").innerText;
            document.all.item("ErrorMessageLabel").innerText = "";
            alert(strMSG);
        }
}
       
window.onload= function(){ setTab('td', 1, 2);fnSetTab(1);};

//        function windows_load() 
//        {
//            
//                                
//        }

        function fnSetTab(tabIndex) 
        {
            var intLoop;          
            for (intLoop = 1; intLoop <= 2; intLoop++) 
            {
                if (document.getElementById("divTab" + intLoop)) 
                {
                    document.all.item("divTab" + intLoop).style.display = "none";
                }
            }
            
            document.all.item("divTab" + tabIndex).style.display = "block";
        }

        function setTab(tabPrefix, tabIndex, tabCount) 
        {
            var intLoop;
            for (intLoop = 1; intLoop <= tabCount; intLoop++) 
            {
                if (document.getElementById(tabPrefix + intLoop)) 
                {
                    
                    document.getElementById(tabPrefix + intLoop).className = "clsInactiveTab";                
//           nTD
                }

            }
              
            document.getElementById("td" + tabIndex).className = "clsActiveTab";
//            nTD1
            
        }
         