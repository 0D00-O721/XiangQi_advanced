package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

//画棋盘和棋子
public final class BoardPanel extends JPanel{
    private static final long serialVersionUID=1L;

    static final int CELL=54;
    static final int MG=66;
    static final int PAD=34;
    static final int R=23;
    static final int HR=26;
    static final int MS=190;

    static final Color BG=new Color(38,30,24);
    static final Color W1=new Color(249,232,200);
    static final Color W2=new Color(226,196,152);
    static final Color LN=new Color(112,78,46);
    static final Color REDC=new Color(178,38,38);
    static final Color BLC=new Color(48,44,40);
    static final Color AC=new Color(31,128,122);//选中
    static final Color MK=new Color(196,141,56);//mark

    static final Font pf=kai(Font.BOLD,28);// piece
    static final Font rf=kai(Font.BOLD,23);// river

    final Board b;
    final Timer tm;

    int sr=-1,sc=-1;
    long t0;
    int hr=-1,hc=-1;

    int f0r=-1,f0c=-1,t1r=-1,t1c=-1;

    final List<Point> pts=new ArrayList<>();

    Piece mv,cap;
    double x0,y0,x1,y1;
    long ta;
    boolean ani;

    int cnt;

    public BoardPanel(Board b){
        this.b=b;
        setBackground(BG);
        setPreferredSize(new Dimension(MG*2+CELL*(Board.COLS-1),MG*2+CELL*(Board.ROWS-1)));
        tm=new Timer(16,e->tick());//from Swing
    }

    static Font kai(int st,int sz){
        return new Font("KaiTi",st,sz);
    }

    //点击位置换算成格子
    public Point at(int x,int y){
        int c=(int)Math.round((x-ox())/(double)CELL);
        int r=(int)Math.round((y-oy())/(double)CELL);
        if(!b.isInside(r,c))return null;
        int dx=x-sx(c),dy=y-sy(r);
        if(dx*dx+dy*dy>HR*HR)return null;//HR:可点半径
        return new Point(c,r);
    }

    public void sel(int row,int col){// 可移动到的点
        sr=row;sc=col;
        t0=System.currentTimeMillis();
        pts.clear();
        Piece p=b.getPieceAt(row,col);
        if(p!=null){
            for(int r=0;r<Board.ROWS;r++){
                for(int c=0;c<Board.COLS;c++){
                    if(r==row&&c==col)continue;
                    Piece t=b.getPieceAt(r,c);
                    if(t!=null&&t.isRed()==p.isRed())continue;
                    if(p.canMove(b,row,col,r,c))pts.add(new Point(c,r));
                }
            }
        }
        upd();
        repaint();
    }

    public void hv(int row,int col){
        if(row==hr&&col==hc)return;
        hr=row;hc=col;
        repaint();
    }

    //走子动画
    public void moveAnim(Piece p,Piece cp,int fr,int fc,int tr,int tc){
        sr=-1;sc=-1;
        pts.clear();
        f0r=fr;f0c=fc;t1r=tr;t1c=tc;
        if(p==null){
            upd();repaint();
            return;
        }
        mv=p;
        cap=cp;
        x0=sx(fc);y0=sy(fr);
        x1=sx(tc);y1=sy(tr);
        ta=System.currentTimeMillis();
        ani=true;
        upd();
        fire();
        repaint();
    }

    public void resetState(){
        tm.stop();
        ani=false;
        mv=null;cap=null;
        sr=-1;sc=-1;
        hr=-1;hc=-1;
        f0r=-1;f0c=-1;t1r=-1;t1c=-1;
        pts.clear();
        fire();
        repaint();
    }

    private void fire(){
        firePropertyChange("redTurn",cnt,++cnt);
    }

    private void tick(){
        if(ani&&System.currentTimeMillis()-ta>=MS){
            ani=false;
            mv=null;
            cap=null;
        }
        upd();
        repaint();
    }

    private void upd(){
        boolean need=ani||sr>=0;
        if(need&&!tm.isRunning())tm.start();
        else if(!need&&tm.isRunning())tm.stop();
    }

    @Override
    protected void paintComponent(Graphics gg){
        super.paintComponent(gg);
        Graphics2D g=(Graphics2D)gg.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);

        bg(g);
        card(g);
        grid(g);
        river(g);
        last(g);
        hint(g);
        pcs(g);
        hov(g);
        g.dispose();
    }

    private void bg(Graphics2D g){
        g.setColor(BG);
        g.fillRect(0,0,getWidth(),getHeight());
        float rad=Math.max(getWidth(),getHeight())*0.72f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth()/2f,getHeight()/2f),rad,
                new float[]{0f,1f},new Color[]{new Color(94,71,50),BG}));
        g.fillRect(0,0,getWidth(),getHeight());
    }

    private void card(Graphics2D g){
        int x=ox()-PAD,y=oy()-PAD;
        int w=CELL*(Board.COLS-1)+PAD*2;
        int h=CELL*(Board.ROWS-1)+PAD*2;

        g.setColor(new Color(0,0,0,46));
        g.fillRoundRect(x-2,y+6,w+4,h+4,30,30);
        g.setColor(new Color(0,0,0,34));
        g.fillRoundRect(x-5,y+10,w+10,h+8,34,34);

        g.setPaint(new GradientPaint(0,y,W1,0,y+h,W2));
        g.fillRoundRect(x,y,w,h,26,26);

        g.setColor(new Color(255,255,255,70));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x+1,y+1,w-2,h-2,26,26);
        g.setColor(new Color(86,60,36,220));
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(x,y,w,h,26,26);
    }

    private void grid(Graphics2D g){
        g.setColor(LN);
        g.setStroke(new BasicStroke(1.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for(int i=0;i<Board.ROWS;i++)ln(g,i,0,i,Board.COLS-1);
        for(int j=0;j<Board.COLS;j++){
            if(j==0||j==Board.COLS-1)ln(g,0,j,Board.ROWS-1,j);
            else{
                ln(g,0,j,4,j);
                ln(g,5,j,Board.ROWS-1,j);
            }
        }
        //九宫
        ln(g,0,3,2,5);ln(g,0,5,2,3);
        ln(g,7,3,9,5);ln(g,7,5,9,3);

        g.setStroke(new BasicStroke(2.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.drawRect(ox(),oy(),CELL*(Board.COLS-1),CELL*(Board.ROWS-1));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(ox()-27,oy()-27,CELL*(Board.COLS-1)+54,CELL*(Board.ROWS-1)+54);

        //炮位、兵位
        mark(g,2,1);mark(g,2,7);
        mark(g,7,1);mark(g,7,7);
        for(int j=0;j<Board.COLS;j+=2){
            mark(g,3,j);
            mark(g,6,j);
        }
    }

    private void mark(Graphics2D g,int row,int col){
        int x=sx(col),y=sy(row);
        int gap=5,len=7;
        int[] d={-1,1};
        for(int a:d){
            for(int c:d){
                if(col==0&&a<0)continue;
                if(col==Board.COLS-1&&a>0)continue;
                int q=x+a*(gap+len),p=y+c*(gap+len);
                int e=x+a*gap,f=y+c*gap;
                g.drawLine(q,f,e,f);
                g.drawLine(e,p,e,f);
            }
        }
    }

    private void river(Graphics2D g){
        g.setFont(rf);
        g.setColor(new Color(112,78,46,185));
        int y=(sy(4)+sy(5))/2;
        txt(g,"楚  河",sx(1)+CELL,y);
        txt(g,"汉  界",sx(7)-CELL,y);
    }

    private void last(Graphics2D g){//last step
        if(t1r<0)return;
        int r=R+4;
        g.setColor(new Color(MK.getRed(),MK.getGreen(),MK.getBlue(),40));
        g.fillOval(sx(f0c)-r,sy(f0r)-r,r*2,r*2);
        g.fillOval(sx(t1c)-r,sy(t1r)-r,r*2,r*2);
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(MK.getRed(),MK.getGreen(),MK.getBlue(),90));
        g.drawOval(sx(f0c)-r,sy(f0r)-r,r*2,r*2);
        g.setColor(new Color(MK.getRed(),MK.getGreen(),MK.getBlue(),170));
        g.drawOval(sx(t1c)-r,sy(t1r)-r,r*2,r*2);
    }

    private void hint(Graphics2D g){
        for(Point h:pts){
            int x=sx(h.x),y=sy(h.y);
            boolean on=(h.x==hc&&h.y==hr);
            Piece t=b.getPieceAt(h.y,h.x);
            if(t==null){
                int r=on?8:6;
                g.setColor(new Color(AC.getRed(),AC.getGreen(),AC.getBlue(),on?215:150));
                g.fillOval(x-r,y-r,r*2,r*2);
            }else{
                int r=R+3;
                g.setColor(new Color(AC.getRed(),AC.getGreen(),AC.getBlue(),on?235:150));
                g.setStroke(new BasicStroke(on?3f:2f));
                g.drawOval(x-r,y-r,r*2,r*2);
            }
        }
    }

    private void pcs(Graphics2D g){
        for(int i=0;i<Board.ROWS;i++){
            for(int j=0;j<Board.COLS;j++){
                Piece p=b.getPieceAt(i,j);
                if(p==null||p==mv)continue;
                int x=sx(j),y=sy(i);
                boolean s=(i==sr&&j==sc);
                if(s)glow(g,x,y);
                pc(g,p,x,y,s?1.05:1.0,1f);
            }
        }
        anim(g);
    }

    private void anim(Graphics2D g){
        if(!ani)return;
        double k=Math.min(1.0,(System.currentTimeMillis()-ta)/(double)MS);

        if(cap!=null){
            float fa=(float)Math.max(0.0,1-k);
            if(fa>0.02f)pc(g,cap,(int)Math.round(x1),(int)Math.round(y1),1-0.18*k,fa);
        }
        if(mv!=null){
            double e=1-Math.pow(1-k,3);
            int x=(int)Math.round(x0+(x1-x0)*e);
            int y=(int)Math.round(y0+(y1-y0)*e);
            int lf=(int)Math.round(Math.sin(Math.PI*k)*7);
            pc(g,mv,x,y-lf,1.06,1f);
        }
    }

    private void hov(Graphics2D g){
        if(ani||sr>=0||hr<0)return;
        Piece p=b.getPieceAt(hr,hc);
        if(p==null||p.isRed()!=b.Redturn)return;
        int x=sx(hc),y=sy(hr),r=R+6;
        g.setColor(new Color(255,250,235,110));
        g.setStroke(new BasicStroke(1.6f));
        g.drawOval(x-r,y-r,r*2,r*2);
    }

    private void glow(Graphics2D g,int x,int y){
        double s=(System.currentTimeMillis()-t0)/1000.0;
        float p=(float)(0.5+0.5*Math.sin(s*3.6));
        int r=R+7+Math.round(p*3);
        for(int i=0;i<3;i++){//3圈
            int rr=r+i;
            g.setColor(new Color(AC.getRed(),AC.getGreen(),AC.getBlue(),30+i*8));
            g.setStroke(new BasicStroke(6f-i*1.5f));
            g.drawOval(x-rr,y-rr,rr*2,rr*2);
        }
        g.setColor(new Color(AC.getRed(),AC.getGreen(),AC.getBlue(),205));
        g.setStroke(new BasicStroke(2.2f));
        g.drawOval(x-r,y-r,r*2,r*2);
    }

    //一枚棋子
    private void pc(Graphics2D g,Piece p,int x,int y,double k,float a){
        Graphics2D g2=(Graphics2D)g.create();
        if(a<1f)g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0f,a)));
        g2.translate(x,y);
        g2.scale(k,k);

        int r=R;
        g2.setColor(new Color(58,38,18,80));
        g2.fillOval(-r+1,-r+3,r*2,r*2);
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(-r*0.35f,-r*0.4f),r*2.1f,
                new float[]{0f,0.68f,1f},
                new Color[]{new Color(255,253,246),new Color(250,238,214),new Color(224,202,165)}));//渐变
        g2.fillOval(-r,-r,r*2,r*2);

        Color ink=p.isRed()?REDC:BLC;
        g2.setColor(ink);
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawOval(-r,-r,r*2,r*2);

        int q=r-4;
        g2.setColor(new Color(ink.getRed(),ink.getGreen(),ink.getBlue(),150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(-q,-q,q*2,q*2);

        g2.setFont(pf);
        g2.setColor(ink);
        txt(g2,p.getName(),0,0);
        g2.dispose();
    }

    private void ln(Graphics2D g,int r1,int c1,int r2,int c2){
        g.drawLine(sx(c1),sy(r1),sx(c2),sy(r2));
    }

    private void txt(Graphics2D g,String s,int x,int y){
        FontMetrics m=g.getFontMetrics();
        g.drawString(s,x-m.stringWidth(s)/2,y+(m.getAscent()-m.getDescent())/2);
    }

    private int ox(){
        return Math.max(MG,(getWidth()-CELL*(Board.COLS-1))/2);
    }

    private int oy(){
        return Math.max(MG,(getHeight()-CELL*(Board.ROWS-1))/2);
    }

    private int sx(int c){
        return ox()+c*CELL;
    }

    private int sy(int r){
        return oy()+r*CELL;
    }
}
