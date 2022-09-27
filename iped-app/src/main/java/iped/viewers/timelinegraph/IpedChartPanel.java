package iped.viewers.timelinegraph;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.JFreeChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.Zoomable;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimePeriod;

import iped.app.ui.App;
import iped.utils.IconUtil;
import iped.viewers.timelinegraph.datasets.TimelineDataset;
import iped.viewers.timelinegraph.model.Minute;
import iped.viewers.timelinegraph.popups.ChartPanelPopupMenu;
import iped.viewers.timelinegraph.popups.DataItemPopupMenu;
import iped.viewers.timelinegraph.popups.PlotPopupMenu;
import iped.viewers.timelinegraph.popups.SeriesAxisPopupMenu;
import iped.viewers.timelinegraph.popups.LegendItemPopupMenu;
import iped.viewers.timelinegraph.popups.TimePeriodSelectionPopupMenu;
import iped.viewers.timelinegraph.popups.TimelineFilterSelectionPopupMenu;
import iped.viewers.timelinegraph.swingworkers.HighlightWorker;

public class IpedChartPanel extends ChartPanel implements KeyListener{
	private Rectangle2D filterIntervalRectangle; //represents the filter selection rectangle while drawing/defining a interval

	Point2D filterIntervalPoint;
	Color filterIntervalFillPaint;

	int lastMouseMoveX=-1;

    IpedChartsPanel ipedChartsPanel = null;

    HashMap<Class<? extends TimePeriod>,ChartTimePeriodConstraint> timePeriodConstraints = new HashMap<Class<? extends TimePeriod>,ChartTimePeriodConstraint>();

    JButton apllyFilters;

    private int filterMask = InputEvent.SHIFT_DOWN_MASK;
	
	boolean useBuffer;
	private double filterTriggerDistance;

	ArrayList<Date[]> definedFilters = new ArrayList<Date[]>();
	ArrayList<String> excludedEvents = new ArrayList<String>();
	private static final String resPath = '/' + App.class.getPackageName().replace('.', '/') + '/';

	Date startFilterDate = null;
	Date endFilterDate = null;
	private Stroke filterLimitStroke;
	private AffineTransform affineTransform;
	Date[] mouseOverDates = null;

    TimelineFilterSelectionPopupMenu timelineSelectionPopupMenu = null;
    PlotPopupMenu domainPopupMenu = null;
    LegendItemPopupMenu legendItemPopupMenu = null;
    DataItemPopupMenu itemPopupMenu = null;
    TimePeriodSelectionPopupMenu timePeriodSelectionPopupMenu = null;
    SeriesAxisPopupMenu seriesAxisPopupMenu = null;
    ChartPanelPopupMenu chartPanelPopupMenu = null;
    
	boolean splitByBookmark=true;
	boolean splitByCategory=false;

	private boolean zoomingStart;
	
	public IpedChartPanel(JFreeChart chart, IpedChartsPanel ipedChartsPanel) {
		super(chart, true);
		useBuffer = true;

		this.ipedChartsPanel = ipedChartsPanel;
		this.setFocusable(true);
		this.addKeyListener(this);
	    this.filterIntervalFillPaint = new Color(0, 0, 255, 43);
	    this.filterLimitStroke = new BasicStroke(3,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_MITER, 1);
	    this.affineTransform = new AffineTransform();
	    this.affineTransform.rotate(Math.toRadians(90), 0, 0);
	    this.affineTransform.scale(0.8, 0.8);
	    
	    ChartTimePeriodConstraint c = new ChartTimePeriodConstraint();
	    c.maxZoomoutRangeSize=ChartTimePeriodConstraint.HOUR_MAX_RANGE_SIZE;
	    c.minZoominRangeSize=ChartTimePeriodConstraint.HOUR_MIN_RANGE_SIZE;
	    timePeriodConstraints.put(Hour.class, c);
	    c = new ChartTimePeriodConstraint();
	    c.maxZoomoutRangeSize=ChartTimePeriodConstraint.MINUTE_MAX_RANGE_SIZE;
	    c.minZoominRangeSize=ChartTimePeriodConstraint.MINUTE_MIN_RANGE_SIZE;
	    timePeriodConstraints.put(Minute.class, c);
	    c = new ChartTimePeriodConstraint();
	    c.maxZoomoutRangeSize=ChartTimePeriodConstraint.SECOND_MAX_RANGE_SIZE;
	    c.minZoominRangeSize=ChartTimePeriodConstraint.SECOND_MIN_RANGE_SIZE;
	    timePeriodConstraints.put(Second.class, c);
	    c = new ChartTimePeriodConstraint();
	    c.maxZoomoutRangeSize=ChartTimePeriodConstraint.MILLISECOND_MAX_RANGE_SIZE;
	    c.minZoominRangeSize=ChartTimePeriodConstraint.MILLISECOND_MIN_RANGE_SIZE;
	    timePeriodConstraints.put(Millisecond.class, c);
	    
		timelineSelectionPopupMenu = new TimelineFilterSelectionPopupMenu(this);
		domainPopupMenu = new PlotPopupMenu(this, ipedChartsPanel.getResultsProvider());
		legendItemPopupMenu = new LegendItemPopupMenu(this);
        itemPopupMenu = new DataItemPopupMenu(this);
    	timePeriodSelectionPopupMenu = new TimePeriodSelectionPopupMenu(ipedChartsPanel);
    	seriesAxisPopupMenu = new SeriesAxisPopupMenu(this);
    	chartPanelPopupMenu = new ChartPanelPopupMenu(this);
        
        IpedChartPanel self = this;
        
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        ImageIcon icon = new ImageIcon(IconUtil.class.getResource(resPath + "down.png"));
        Image img = icon.getImage();
        apllyFilters = new JButton(new ImageIcon(img.getScaledInstance(12, 12,  java.awt.Image.SCALE_SMOOTH)));
        apllyFilters.setMaximumSize(new Dimension(16, 16));
        apllyFilters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chartPanelPopupMenu.show(apllyFilters, apllyFilters.getX()-2,  apllyFilters.getY()+apllyFilters.getHeight()-8);
			}
		});
        this.add(apllyFilters);
	
        this.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseMoved(ChartMouseEvent event) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void chartMouseClicked(ChartMouseEvent event) {
				ChartEntity ce = event.getEntity();
				int x = event.getTrigger().getX();
				if(ce instanceof XYItemEntity) {
					XYItemEntity ie = ((XYItemEntity) ce);
					
					if(ie.getDataset() instanceof TimelineDataset) {
						itemPopupMenu.setChartEntity(ie);
						
						ArrayList<XYItemEntity> entityList = new ArrayList<XYItemEntity>();
						EntityCollection entities = self.getChartRenderingInfo().getEntityCollection();
			            if (entities != null) {
			                int entityCount = entities.getEntityCount();
			                for (int i = entityCount - 1; i >= 0; i--) {
			                    ChartEntity entity = (ChartEntity) entities.getEntity(i);
			                    if(entity instanceof XYItemEntity) {
				                    if (entity.getArea().getBounds().getMaxX()>x && entity.getArea().getBounds().getMinX()<x) {
				                    	entityList.add((XYItemEntity)entity);
				                    }
			                    }
			                }
			            }
						itemPopupMenu.setChartEntityList(entityList);

						itemPopupMenu.show(event.getTrigger().getComponent(), x, event.getTrigger().getY());
					}else {
						
					}
				}

				if(ce instanceof PlotEntity) {
					PlotEntity ie = ((PlotEntity) ce);
					XYPlot plot = (XYPlot) ie.getPlot();

					Date[] filteredDate = self.getDefinedFilter(event.getTrigger().getX());
					if(filteredDate!=null) {
						timelineSelectionPopupMenu.setDates(filteredDate);
						timelineSelectionPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
					}else {
						domainPopupMenu.setDate(new Date((long) ipedChartsPanel.getDomainAxis().java2DToValue(event.getTrigger().getX(), self.getScreenDataArea(), plot.getDomainAxisEdge())));
						domainPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
					}
				}
				
				if(ce instanceof AxisEntity) {
					if(((AxisEntity) ce).getAxis() instanceof DateAxis) {
						timePeriodSelectionPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
					}else {
						seriesAxisPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
					}
				}
				if(ce instanceof JFreeChartEntity) {
					if(event.getTrigger().getX()<self.getScreenDataArea().getMinX()) {
						seriesAxisPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
					}
				}
				
				if(ce instanceof LegendItemEntity) {
					LegendItemEntity le = (LegendItemEntity) ce;
					
					legendItemPopupMenu.setLegendItemEntity(le);
					legendItemPopupMenu.show(event.getTrigger().getComponent(), event.getTrigger().getX(), event.getTrigger().getY());
				}
			}
		});
        
        this.addMouseWheelListener(new IpedTimelineMouseWheelHandler(this));
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		int mods = e.getModifiersEx();
		int x=e.getX();		
		if ((mods & this.filterMask) == this.filterMask){
            setCursor(Cursor.getPredefinedCursor(
                    Cursor.E_RESIZE_CURSOR));
		}else {
			if(lastMouseMoveX!=x) {
	            setCursor(Cursor.getDefaultCursor());
	            Graphics2D g2 = (Graphics2D) getGraphics();
	            if(lastMouseMoveX>=0) {
	                drawDateCursor(g2, lastMouseMoveX, true);
	            }
	            drawDateCursor(g2, x, true);
			}
		}
		super.mouseMoved(e);
	}

	public Date[] findDefinedFilterDates(Date date) {
		for (Date[] dates : definedFilters) {
			if(dates[0].getTime()-1<=date.getTime() && dates[1].getTime()+1>=date.getTime()) {
				return dates;
			}
		}
		return null;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		int mods = e.getModifiersEx();
    	if(((mods & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK)) {
    		if ((mods & this.filterMask) == this.filterMask){
                setCursor(Cursor.getPredefinedCursor(
                        Cursor.E_RESIZE_CURSOR));
            	
        		if (this.filterIntervalRectangle == null) {
        			startFilterDate = new Date((long) ipedChartsPanel.domainAxis.java2DToValue(e.getX(), this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge()));
        			startFilterDate = removeFromDatePart(startFilterDate);
        			
        			Date[] dates = findDefinedFilterDates(startFilterDate);
        			if(dates!=null) {
        				startFilterDate = dates[0];
        				removeFilter(dates);

        				//already exists an interval continuos with this so edits him instead of creating a new one
        	            Graphics2D g2 = (Graphics2D) getGraphics();

        				ipedChartsPanel.paintImmediately(this.getScreenDataArea().getBounds());
        			}

        			int x = (int) ipedChartsPanel.domainAxis.dateToJava2D(startFilterDate,  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());

        			Rectangle2D screenDataArea = getScreenDataArea(x, e.getY());

                    if (screenDataArea != null) {
                        this.filterIntervalPoint = getPointInRectangle(x, e.getY(),
                                screenDataArea);
                    }
                    else {
                        this.filterIntervalPoint = null;
                    }
                }
        		return;
        	}else {
        		zoomingStart=true;
        	}
        }

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		int mods = e.getModifiersEx();
        if ((mods & this.filterMask) == this.filterMask) {
            // if no initial point was set, ignore dragging...
            if (this.filterIntervalPoint == null) {
                return;
            }

            Graphics2D g2 = (Graphics2D) getGraphics();

            // erase the previous zoom rectangle (if any).  We only need to do
            // this is we are using XOR mode, which we do when we're not using
            // the buffer (if there is a buffer, then at the end of this method we
            // just trigger a repaint)
            if (!this.useBuffer) {
                drawFilterIntervalRectangle(g2, true);
            }

            Rectangle2D scaledDataArea = getScreenDataArea(
                    (int) this.filterIntervalPoint.getX(), (int) this.filterIntervalPoint.getY());

            endFilterDate = new Date((long) ipedChartsPanel.domainAxis.java2DToValue(e.getX(), this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge()));
            endFilterDate = lastdateFromDatePart(endFilterDate);
            
			Date[] dates = findDefinedFilterDates(endFilterDate);
			if(dates!=null) {
				removeFilter(dates);
				ipedChartsPanel.paintImmediately(this.getScreenDataArea().getBounds());
			}
            
			int x = (int) ipedChartsPanel.domainAxis.dateToJava2D(endFilterDate,  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());

            double xmax = Math.min(x, scaledDataArea.getMaxX());

            this.filterIntervalRectangle = new Rectangle2D.Double(
            		this.filterIntervalPoint.getX(), scaledDataArea.getMinY(),
                    xmax - this.filterIntervalPoint.getX(), scaledDataArea.getHeight());

            // Draw the new zoom rectangle...
            if (this.useBuffer) {
                repaint();
            }
            else {
                // with no buffer, we use XOR to draw the rectangle "over" the
                // chart...
            	drawFilterIntervalRectangle(g2, true);
            }
            
            g2.dispose();
    		
    		return;
        }

        if(zoomingStart) {
        	//tricks the parent event handler with a new mouse event where the Y is the bottom of the chart ((int)scaledDataArea.getMaxY())
            Rectangle2D scaledDataArea = getScreenDataArea(
                    (int) e.getX(), (int) e.getY());
            MouseEvent en = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), e.getX(), (int)scaledDataArea.getMaxY(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
    		super.mouseDragged(en);	
        }else {
    		super.mouseDragged(e);	
        }
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int mods = e.getModifiersEx();
		zoomingStart=false;

        if ((mods & this.filterMask) != this.filterMask) {
    		super.mouseReleased(e);
        }

        if (this.filterIntervalRectangle != null) {
            boolean filterTrigger1 = Math.abs(e.getX()
                - this.filterIntervalPoint.getX()) >= this.filterTriggerDistance;

            if (filterTrigger1) {
                if ((e.getX() < this.filterIntervalPoint.getX())) {
                	//do nothing (the firt date should be lower than the last)
                    //restoreAutoBounds();
                }
                else {
                    Date[] filterDates = new Date[2];
                    filterDates[0]=startFilterDate;
                    filterDates[1]=endFilterDate;
                    definedFilters.add(filterDates);

					timelineSelectionPopupMenu.setDates(filterDates);
					timelineSelectionPopupMenu.show(e.getComponent(), e.getX(), e.getY());

                    mouseOverDates=filterDates;
                }
                this.filterIntervalPoint = null;
                this.filterIntervalRectangle = null;
                this.setRefreshBuffer(true);
                repaint();
            }
            else {
                // erase the zoom rectangle
                Graphics2D g2 = (Graphics2D) getGraphics();
                if (this.useBuffer) {
                    repaint();
                }
                else {
                	drawFilterIntervalRectangle(g2, true);
                }
                g2.dispose();
                this.filterIntervalPoint = null;
                this.filterIntervalRectangle = null;
            }

            setCursor(Cursor.getDefaultCursor());
        }
        else if (e.isPopupTrigger()) {
            if (this.getPopupMenu() != null) {
                displayPopupMenu(e.getX(), e.getY());
            }
        }
	}
	
	public void addFilter(Date startDate, Date endDate) {
        Date[] filterDates = new Date[2];
        filterDates[0]=startDate;
        filterDates[1]=endDate;
        definedFilters.add(filterDates);
	}

    /**
     * Draws zoom rectangle (if present).
     * The drawing is performed in XOR mode, therefore
     * when this method is called twice in a row,
     * the second call will completely restore the state
     * of the canvas.
     *
     * @param g2 the graphics device.
     * @param xor  use XOR for drawing?
     */
    private void drawFilterIntervalRectangle(Graphics2D g2, boolean xor) {
        if (this.filterIntervalRectangle != null) {
            String strStartDate = ipedChartsPanel.getDomainAxis().ISO8601DateFormat(startFilterDate);
            String strEndDate = ipedChartsPanel.getDomainAxis().ISO8601DateFormat(endFilterDate); 
        	
        	int w = (int) this.filterIntervalRectangle.getMaxX()-(int) this.filterIntervalRectangle.getMinX();
            int h = (int) this.filterIntervalRectangle.getMaxY()-(int) this.filterIntervalRectangle.getMinY();
            BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg2 = bimage.createGraphics();
            bg2.setColor(Color.GRAY);
            bg2.fillRect(0, 0, w, h);
            
            bg2.setColor(Color.WHITE);
            AffineTransform at = (AffineTransform ) affineTransform.clone();
            at.scale(1/0.8, 1/0.8);
            bg2.setFont(g2.getFont().deriveFont(at));//rotate text 90
            bg2.setStroke(this.filterLimitStroke);
            bg2.drawString(strStartDate, 0, 0);
            bg2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER));
            
            
            bg2.dispose();
            
            if (xor) {
                // Set XOR mode to draw the zoom rectangle
               g2.setXORMode(Color.WHITE);
           }
            g2.drawImage(bimage, (int) this.filterIntervalRectangle.getMinX(),(int) this.filterIntervalRectangle.getMinY(),null);
            g2.setPaint(Color.BLACK);
            g2.setStroke(this.filterLimitStroke);
            g2.setFont(g2.getFont().deriveFont(at));//rotate text 90
            g2.drawLine((int) this.filterIntervalRectangle.getMaxX(),(int) this.filterIntervalRectangle.getMinY(),(int) this.filterIntervalRectangle.getMaxX(),(int) this.filterIntervalRectangle.getMaxY());
            g2.drawString(strEndDate, (int) this.filterIntervalRectangle.getMaxX()+2, (int) this.filterIntervalRectangle.getMinY()+g2.getFontMetrics().getHeight()+2);
            if (xor) {
                // Reset to the default 'overwrite' mode
                g2.setPaintMode();
            }
            
        }
    }

    public Date removeNextFromDatePart(Date date) {
    	RegularTimePeriod t;
		try {
			t = (RegularTimePeriod) ipedChartsPanel.getTimePeriodClass().getConstructor(Date.class).newInstance(date);
	    	return t.next().getStart();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    	
    	return new Date(1999,0,1);
    }

    public Date removeFromDatePart(Date date) {
    	TimePeriod t;
		try {
			t = ipedChartsPanel.getTimePeriodClass().getConstructor(Date.class).newInstance(date);
	    	return t.getStart();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    	
    	return new Date(1999,0,1);
    }
    
    public Date removeFromDatePart(Date date, int fromDatePart) {
        Calendar cal = Calendar.getInstance(ipedChartsPanel.getTimeZone());
        cal.setTime(date);
        
        if(fromDatePart==Calendar.DAY_OF_MONTH) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
        return cal.getTime();
    }

    public Date lastdateFromDatePart(Date date) {
    	TimePeriod t;
		try {
			t = ipedChartsPanel.getTimePeriodClass().getConstructor(Date.class).newInstance(date);
	    	return t.getEnd();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
    	
    	return new Date(1999,0,1);
    }
    
    public Date lastdateFromDatePart(Date date, int fromDatePart) {
        Calendar cal = Calendar.getInstance(ipedChartsPanel.getTimeZone());
        cal.setTime(date);
        
        if(fromDatePart==Calendar.DAY_OF_MONTH) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
        }
        return cal.getTime();
    }
    

    /**
     * Draws zoom rectangle (if present).
     * The drawing is performed in XOR mode, therefore
     * when this method is called twice in a row,
     * the second call will completely restore the state
     * of the canvas.
     *
     * @param g2 the graphics device.
     * @param xor  use XOR for drawing?
     */
    private void drawDateCursor(Graphics2D g2, int x, boolean xor) {
        if (xor) {
            // Set XOR mode to draw the zoom rectangle
           g2.setXORMode(Color.GRAY);
       }
       Rectangle2D screenDataArea = getScreenDataArea(
               x,
               2);
       
       double maxX = screenDataArea.getMaxX();
       double minX = screenDataArea.getMinX();
       if((x>minX)&&(x<maxX)) {
    	   
           double minY = screenDataArea.getMinY();
           double maxY = screenDataArea.getMaxY();

           double h = screenDataArea.getHeight();

           Date correspondingDate = new Date((long) ipedChartsPanel.domainAxis.java2DToValue(x, this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge()));
           correspondingDate = removeFromDatePart(correspondingDate);

           String strDate = ipedChartsPanel.getDomainAxis().ISO8601DateFormat(correspondingDate); 
           

           int w = 50;

           BufferedImage bimage = new BufferedImage(w, (int) h, BufferedImage.TYPE_INT_ARGB);
           Graphics2D bg2 = bimage.createGraphics();
           bg2.setColor(Color.BLACK);
           AffineTransform at = (AffineTransform ) affineTransform.clone();
           at.scale(1/0.8, 1/0.8);
           bg2.setFont(g2.getFont().deriveFont(at));//rotate text 90
           bg2.setStroke(this.filterLimitStroke);
           bg2.drawString(strDate, 2, 2);
           bg2.drawLine(0,0,0,(int)maxY-(int)minY);
           bg2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER));
           
           bg2.dispose();
           
           g2.drawImage(bimage, x, (int) minY,null);
    	   
       }

       lastMouseMoveX=x;

       if (xor) {
           // Reset to the default 'overwrite' mode
           g2.setPaintMode();
       }
    }
    
    
	private void drawDefinedFiltersDates(Date[] dates, Graphics2D g2, boolean xor) {
        if (xor) {
            // Set XOR mode to draw the zoom rectangle
           g2.setXORMode(Color.GRAY);
       }

        String strStartDate = ipedChartsPanel.getDomainAxis().dateToString(dates[0]); 
        String strEndDate = ipedChartsPanel.getDomainAxis().dateToString(dates[1]);
        
        int xstart = (int) ipedChartsPanel.domainAxis.dateToJava2D(dates[0],  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());
        int xend = (int) ipedChartsPanel.domainAxis.dateToJava2D(dates[1],  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());

        Rectangle2D screenDataArea = getScreenDataArea(
                xend,
                2);

        double minX = screenDataArea.getMinX();
        double maxX = screenDataArea.getMaxX();
        double maxY = screenDataArea.getMaxY();

        int x = (int) Math.max(minX, xstart);
        int y = (int) screenDataArea.getMinY();
        int w = (int) Math.min(xend-x,
                maxX - x);
        double h = screenDataArea.getHeight();

        Rectangle2D rectangle2d = new Rectangle2D.Double(x, y, w, h);
        
        g2.setPaint(Color.BLACK);
        g2.setStroke(this.filterLimitStroke);
        g2.setFont(g2.getFont().deriveFont(affineTransform));//rotate text 90
        g2.drawString(strStartDate, (int) rectangle2d.getMinX()+2, (int) rectangle2d.getMinY()+g2.getFontMetrics().getHeight()+2);
        g2.drawString(strEndDate, (int) rectangle2d.getMaxX()+2, (int) rectangle2d.getMinY()+g2.getFontMetrics().getHeight()+2);
       
       if (xor) {
           // Reset to the default 'overwrite' mode
           g2.setPaintMode();
       }
	}

	/**
     * Draws defined filters dates text (if present).
     * The drawing is performed in XOR mode, therefore
     * when this method is called twice in a row,
     * the second call will completely restore the state
     * of the canvas.
     *
     * @param g2 the graphics device.
     * @param xor  use XOR for drawing?
     */
    private void drawDefinedFiltersDates(Graphics2D g2, boolean xor) {
    	for (Date[] dates : this.definedFilters) {
    		drawDefinedFiltersDates(dates, g2, xor);
		}
    	
    }    

    /**
     * Returns the data area (the area inside the axes) for the plot or subplot,
     * with the current scaling applied.
     *
     * @param x  the x-coordinate (for subplot selection).
     * @param y  the y-coordinate (for subplot selection).
     *
     * @return The scaled data area.
     */
    public Rectangle2D getScreenDataArea(int x, int y) {
        PlotRenderingInfo plotInfo = this.getChartRenderingInfo().getPlotInfo();
        Rectangle2D result;
        if (plotInfo.getSubplotCount() == 0) {
            result = getScreenDataArea();
        }
        else {
            // get the origin of the zoom selection in the Java2D space used for
            // drawing the chart (that is, before any scaling to fit the panel)
            Point2D selectOrigin = translateScreenToJava2D(new Point(x, y));
            int subplotIndex = plotInfo.getSubplotIndex(selectOrigin);
            if (subplotIndex == -1) {
                return scale(plotInfo.getSubplotInfo(0).getDataArea());
            }
            result = scale(plotInfo.getSubplotInfo(subplotIndex).getDataArea());
        }
        return result;
    }
    
    public Date[] getDefinedFilter(int x) {
    	for (Date[] dates : definedFilters) {
            int xstart = (int) ipedChartsPanel.domainAxis.dateToJava2D(dates[0],  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());
            int xend = (int) ipedChartsPanel.domainAxis.dateToJava2D(dates[1],  this.getScreenDataArea(), ipedChartsPanel.combinedPlot.getDomainAxisEdge());
            if((xstart<=x)&& (x<=xend)) {
            	return dates;
            }
		}
    	return null;
    }

    /**
     * Returns a point based on (x, y) but constrained to be within the bounds
     * of the given rectangle.  This method could be moved to JCommon.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param area  the rectangle ({@code null} not permitted).
     *
     * @return A point within the rectangle.
     */
    private Point2D getPointInRectangle(int x, int y, Rectangle2D area) {
        double xx = Math.max(area.getMinX(), Math.min(x, area.getMaxX()));
        double yy = Math.max(area.getMinY(), Math.min(y, area.getMaxY()));
        return new Point2D.Double(xx, yy);
    }

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            setCursor(Cursor.getPredefinedCursor(
                    Cursor.E_RESIZE_CURSOR));
        }
	}

	@Override
	public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
        	setCursor(Cursor.getDefaultCursor());
        }
	}
	
	public void removeFilter(Date[] removedDates) {
		definedFilters.remove(removedDates);
		HighlightWorker sw = new HighlightWorker(ipedChartsPanel.getDomainAxis(),ipedChartsPanel.resultsProvider, removedDates[0], removedDates[1], false, false);
		for (Date[] dates : definedFilters) {
			ipedChartsPanel.highlightItemsOnInterval(dates[0],dates[1],false);
		}
	}

	public void removeAllFilters() {
		for (Date[] removedDates : definedFilters) {
			HighlightWorker sw = new HighlightWorker(ipedChartsPanel.getDomainAxis(),ipedChartsPanel.resultsProvider, removedDates[0], removedDates[1], false, false);
		}
		definedFilters.clear();
		while(excludedEvents.size()>0) {
			excludedEvents.remove(excludedEvents.size()-1);
		}
		ipedChartsPanel.setApplyFilters(false);
		filterSelection();
		IpedCombinedDomainXYPlot rootPlot = ((IpedCombinedDomainXYPlot) getChart().getPlot());
		List<XYPlot> xyPlots = rootPlot.getSubplots();

		for (XYPlot xyPlot : xyPlots) {
			for(int i=0; i<xyPlot.getDataset(0).getSeriesCount(); i++) {
				String currSeries = (String) xyPlot.getDataset(0).getSeriesKey(i);
				rootPlot.getRenderer().setSeriesVisible(i, true, true);
			}
		}
		
		App app = (App) ipedChartsPanel.getGUIProvider();
		app.setDockablesColors();
		this.repaint();
	}

	public IpedChartsPanel getIpedChartsPanel() {
		return ipedChartsPanel;
	}

	@Override
	public void mouseExited(MouseEvent e) {
        Graphics2D g2 = (Graphics2D) getGraphics();
        drawDateCursor(g2, lastMouseMoveX, true);
		lastMouseMoveX=-1;
		super.mouseExited(e);
	}

	@Override
	public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) getGraphics();
        drawDateCursor(g2, lastMouseMoveX, true);
		lastMouseMoveX=-1;
		super.paint(g);

	}

	@Override
	public Graphics getGraphics() {
		Graphics g = super.getGraphics();
		
		if(g instanceof IpedGraphicsWrapper) {
			return g;
		}else {
			return new IpedGraphicsWrapper((Graphics2D) g);
		}
	}

	public boolean getSplitByBookmark() {
		return splitByBookmark;
	}

	public void setSplitByBookmark(boolean breakByBookmark) {
		if(breakByBookmark) {
			splitByCategory=false;
		}
		this.splitByBookmark = breakByBookmark;
	}

	public boolean getSplitByCategory() {
		return splitByCategory;
	}

	public void setSplitByCategory(boolean breakByCategory) {
		if(breakByCategory) {
			splitByBookmark=false;
		}
		this.splitByCategory = breakByCategory;
	}

	public ArrayList<Date[]> getDefinedFilters() {
		return definedFilters;
	}

	public void setDefinedFilters(ArrayList<Date[]> definedFilters) {
		this.definedFilters = definedFilters;
	}

	public void filterSelection() {
		this.getIpedChartsPanel().setInternalUpdate(true);
		App app = (App) this.getIpedChartsPanel().getResultsProvider();			
		app.getAppListener().updateFileListing();
		app.setDockablesColors();
	}

	public ArrayList<String> getExcludedEvents() {
		return excludedEvents;
	}

	public void setExcludedEvents(ArrayList<String> excludedEvents) {
		this.excludedEvents = excludedEvents;
	}

	public boolean hasNoFilter() {
		return (definedFilters.size()==0)&&(excludedEvents.size()==0);
	}

	@Override
	public void zoom(Rectangle2D selection) {
        // get the origin of the zoom selection in the Java2D space used for
        // drawing the chart (that is, before any scaling to fit the panel)
        Point2D selectOrigin = translateScreenToJava2D(new Point(
                (int) Math.ceil(selection.getX()),
                (int) Math.ceil(selection.getY())));
        PlotRenderingInfo plotInfo = this.getChartRenderingInfo().getPlotInfo();
        Rectangle2D scaledDataArea = getScreenDataArea(
                (int) selection.getCenterX(), (int) selection.getCenterY());
        if ((selection.getHeight() > 0) && (selection.getWidth() > 0)) {

            double hLower = (selection.getMinX() - scaledDataArea.getMinX())
                / scaledDataArea.getWidth();
            double hUpper = (selection.getMaxX() - scaledDataArea.getMinX())
                / scaledDataArea.getWidth();
            double vLower = (scaledDataArea.getMaxY() - selection.getMaxY())
                / scaledDataArea.getHeight();
            double vUpper = (scaledDataArea.getMaxY() - selection.getMinY())
                / scaledDataArea.getHeight();

            Plot p = this.getChart().getPlot();
            if (p instanceof Zoomable) {
                // here we tweak the notify flag on the plot so that only
                // one notification happens even though we update multiple
                // axes...
                boolean savedNotify = p.isNotify();
                p.setNotify(false);
                Zoomable z = (Zoomable) p;
                if (z.getOrientation() == PlotOrientation.HORIZONTAL) {
                    z.zoomDomainAxes(vLower, vUpper, plotInfo, selectOrigin);
                    z.zoomRangeAxes(hLower, hUpper, plotInfo, selectOrigin);
                }
                else {
                    z.zoomDomainAxes(hLower, hUpper, plotInfo, selectOrigin);
                    z.zoomRangeAxes(vLower, vUpper, plotInfo, selectOrigin);
                }
                p.setNotify(savedNotify);
            }

        }
//		super.zoom(selection);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
        drawFilterIntervalRectangle((Graphics2D) g, true);
	}

	public ChartTimePeriodConstraint getTimePeriodConstraints(Class<? extends TimePeriod> t) {
		if(t!=null) {
			return timePeriodConstraints.get(t);
		}else {
			return timePeriodConstraints.get(this.getIpedChartsPanel().getTimePeriodClass());
		}
	}

	public void setTimePeriodConstraints(Class<? extends TimePeriod> t, ChartTimePeriodConstraint timePeriodConstraint) {
		this.timePeriodConstraints.put(t, timePeriodConstraint);
	}

	public ChartTimePeriodConstraint getTimePeriodConstraints() {
		return timePeriodConstraints.get(this.getIpedChartsPanel().getTimePeriodClass());
	}
}
