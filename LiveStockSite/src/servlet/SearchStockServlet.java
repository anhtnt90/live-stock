package servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import general.Order;
import general.Stock;
import general.UserAccount;
import utils.MyUtils;
 
@WebServlet(urlPatterns = { "/search_stocks" })
public class SearchStockServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
 
    public SearchStockServlet() {
        super();
    }
 
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        
        // Check User has logged on
        UserAccount loginedUser = MyUtils.getLoginedUser(session);
        System.out.println("Logged in user is " + loginedUser);
        
        List<Stock> list = new ArrayList<Stock>();
        Connection conn = MyUtils.getStoredConnection(request);
        int id = loginedUser.getId();
        String table = "Search Results";
        System.out.println(table);
        System.out.println("Userid: "+id);
        String stockType = request.getParameter("stocktype");
        System.out.println("Type: "+stockType);
        String stockKeyword = request.getParameter("stockkeyword");
        System.out.println("Keyword: "+stockKeyword);
        List<String> stockSymbol = new ArrayList<String>();
        List<Order> order_list = new ArrayList<Order>();
        if (stockKeyword!=null) {
        	stockKeyword = stockKeyword.replaceAll("\\s", "");
        }
        try{
        	
        	String sql1;
        	PreparedStatement pstm1 = null;
        	java.sql.ResultSet rs = null;
            if(stockType!=null) {
            	sql1 = "CALL getStockUsingType(?)";
            	pstm1 = conn.prepareStatement(sql1);
            	pstm1.setString(1,stockType);
                rs = pstm1.executeQuery();
            }
            else if ((stockKeyword!=null) & (stockKeyword!="")) {
                String keyword[] = stockKeyword.split(",");
                for (String s: keyword)
                	System.out.println(s);
        		sql1 = "SELECT S.* "
        				+ "FROM STOCK S "
        				+ "WHERE";
            	for (int i=1; i<=keyword.length ; i++) {
            		if (i>1)
            			sql1 += " AND";
            		sql1 +=  " S.StockName LIKE CONCAT('%',?,'%')";
            	}
            	sql1 += ";";
            	System.out.println(sql1);
            	pstm1 = conn.prepareStatement(sql1);
            	for (int i=1; i<=keyword.length ; i++) {
            		pstm1.setString(i,keyword[i-1]);
            	}
            	rs = pstm1.executeQuery();
        				
            }

            if (rs!=null) {
                while (rs.next()) {
                    String sksym = rs.getString("StockSymbol");
                    stockSymbol.add(sksym);
                    String sknm = rs.getString("StockName");
                    String sktp = rs.getString("StockType");
                    float shpr = rs.getFloat("SharePrice");
                    int numsh = rs.getInt("NumAvailShares");
                    Stock data = new Stock(sksym, sknm, sktp, shpr, numsh);
                    list.add(data);
                    System.out.println("Obtained Data: "+sksym+" "+sknm+" "+sktp+" "+shpr+" "+numsh);
                }
            }

            System.out.println("After while");
        } catch (Exception e) {
			e.printStackTrace();
		}
		
        try {
			
			for (String s: stockSymbol) {
				String sql1 = "CALL getMostRecentOrderInfo(?,?)";
				PreparedStatement pstm1 = conn.prepareStatement(sql1);
				pstm1.setInt(1, id);
				pstm1.setString(2, s);
				java.sql.ResultSet rs;
				rs = pstm1.executeQuery();

				while (rs.next()) {
					Order order = new Order();
					order.setId(rs.getInt("OrderId"));
					order.setOrderType(rs.getString("OrderType"));
					order.setTimestamp(rs.getTimestamp("Timestamp_"));
					order.setCusAccNum(rs.getInt("CusAccNum"));
					order.setStockSymbol(rs.getString("StockSymbol"));
					order.setNumShares(rs.getInt("NumShares"));
					order.setPriceType(rs.getString("PriceType"));
					order.setStopPrice(rs.getFloat("StopPrice"));
					order.setRecorded(rs.getBoolean("Recorded"));
					order.setCompleted(rs.getBoolean("Completed"));
					order_list.add(order);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
        
        request.setAttribute("stocks", list);
        request.setAttribute("orders", order_list);
        request.setAttribute("table", table);
        request.setAttribute("userType", loginedUser.getUserType());
        RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/WEB-INF/views/cust_stocks.jsp");
        dispatcher.forward(request, response);
        
    }
 
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
 
}
