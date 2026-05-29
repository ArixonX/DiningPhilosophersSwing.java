import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophersSwing extends JPanel {

    enum State {
        THINKING, WAITING_FOR_FORK_1, WAITING_FOR_FORK_2, EATING
    }

    static class Fork {
        int id;
        ReentrantLock lock = new ReentrantLock();
        volatile int ownerId = -1;

        public Fork(int id) {
            this.id = id;
        }
    }

    class Philosopher implements Runnable {
        int id;
        Fork leftFork, rightFork;
        volatile State state = State.THINKING;
        volatile int eatCount = 0;
        volatile boolean running = true;
        Random random = new Random();

        public Philosopher(int id, Fork leftFork, Fork rightFork) {
            this.id = id;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        public void stopThread() { running = false; }

        @Override
        public void run() {
            try {
                while (running) {
                    state = State.THINKING;
                    int thinkingTime = random.nextInt(5000);
                    for (int t = 0; t < thinkingTime; t += 100) {
                        if (!running) break;
                        while (simulationPaused) {
                            if (!running) break;
                            Thread.sleep(100);
                        }
                        Thread.sleep(Math.min(100, thinkingTime - t));
                    }

                    if (!running) continue;

                    Fork firstFork = (id % 2 == 0) ? leftFork : rightFork;
                    Fork secondFork = (id % 2 == 0) ? rightFork : leftFork;

                    state = State.WAITING_FOR_FORK_1;
                    while (true) {
                        if (!running) return;
                        if (simulationPaused) {
                            Thread.sleep(100);
                            continue;
                        }
                        if (firstFork.lock.tryLock()) break;
                        Thread.sleep(100);
                    }
                    firstFork.ownerId = this.id;

                    Thread.sleep(random.nextInt(200));
                    if (!running) {
                        firstFork.ownerId = -1;
                        firstFork.lock.unlock();
                        return;
                    }

                    state = State.WAITING_FOR_FORK_2;
                    while (true) {
                        if (!running) {
                            firstFork.ownerId = -1;
                            firstFork.lock.unlock();
                            return;
                        }
                        if (simulationPaused) {
                            Thread.sleep(100);
                            continue;
                        }
                        if (secondFork.lock.tryLock()) break;
                        Thread.sleep(100);
                    }
                    secondFork.ownerId = this.id;

                    state = State.EATING;
                    eatCount++;
                    int eatingTime = random.nextInt(1000);
                    for (int t = 0; t < eatingTime; t += 100) {
                        if (!running) break;
                        while (simulationPaused) {
                            if (!running) break;
                            Thread.sleep(100);
                        }
                        Thread.sleep(Math.min(100, eatingTime - t));
                    }

                    firstFork.ownerId = -1;
                    firstFork.lock.unlock();

                    secondFork.ownerId = -1;
                    secondFork.lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    int numPhilosophers = 5;
    final int MAX_PHILOSOPHERS = 7;
    List<Philosopher> philosophers = new ArrayList<>();
    List<Fork> forks = new ArrayList<>();
    List<Thread> threads = new ArrayList<>();
    volatile boolean simulationPaused = false;

    public DiningPhilosophersSwing() {
        setBackground(new Color(24, 24, 28));
        initSystem(numPhilosophers);
        javax.swing.Timer timer = new javax.swing.Timer(25, e -> repaint());
        timer.start();
    }

    private void initSystem(int n) {
        for (Philosopher p : philosophers) p.stopThread();
        philosophers.clear();
        forks.clear();
        threads.clear();
        this.numPhilosophers = n;

        for (int i = 0; i < n; i++) forks.add(new Fork(i));
        for (int i = 0; i < n; i++) {
            Fork left = forks.get((i - 1 + n) % n);
            Fork right = forks.get(i);
            Philosopher p = new Philosopher(i, left, right);
            philosophers.add(p);
            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int tableRadius = Math.min(getWidth(), getHeight()) / 5;

        g2d.setPaint(new RadialGradientPaint(cx, cy, tableRadius + 50, new float[]{0.0f, 1.0f}, new Color[]{new Color(45, 38, 34, 100), new Color(24, 24, 28, 0)}));
        g2d.fillOval(cx - tableRadius - 50, cy - tableRadius - 50, (tableRadius + 50) * 2, (tableRadius + 50) * 2);

        GradientPaint tableGradient = new GradientPaint(cx, cy - tableRadius, new Color(54, 43, 40), cx, cy + tableRadius, new Color(34, 28, 26));
        g2d.setPaint(tableGradient);
        g2d.fillOval(cx - tableRadius, cy - tableRadius, tableRadius * 2, tableRadius * 2);
        g2d.setColor(new Color(75, 60, 55));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(cx - tableRadius, cy - tableRadius, tableRadius * 2, tableRadius * 2);

        double angleStep = 2 * Math.PI / numPhilosophers;

        for (int i = 0; i < numPhilosophers; i++) {
            Fork fork = forks.get(i);
            double forkAngle = i * angleStep + (angleStep / 2.0);
            int fx, fy;
            double angleForLine;

            if (fork.ownerId != -1) {
                double ownerAngle = fork.ownerId * angleStep;
                double heldAngle = ownerAngle + (fork.id == fork.ownerId ? 0.15 : -0.15);
                angleForLine = heldAngle;

                fx = cx + (int) ((tableRadius + 15) * Math.cos(heldAngle));
                fy = cy + (int) ((tableRadius + 15) * Math.sin(heldAngle));

                g2d.setColor(new Color(241, 196, 15));
                g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                angleForLine = forkAngle;
                fx = cx + (int) ((tableRadius - 25) * Math.cos(forkAngle));
                fy = cy + (int) ((tableRadius - 25) * Math.sin(forkAngle));
                g2d.setColor(new Color(149, 165, 166));
                g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }

            int length = 20;
            int ex = fx - (int) (length * Math.cos(angleForLine));
            int ey = fy - (int) (length * Math.sin(angleForLine));
            g2d.drawLine(fx, fy, ex, ey);
            g2d.fillOval(fx - 3, fy - 3, 6, 6);
        }

        for (int i = 0; i < numPhilosophers; i++) {
            Philosopher p = philosophers.get(i);
            double angle = i * angleStep;

            int plateX = cx + (int) ((tableRadius - 40) * Math.cos(angle));
            int plateY = cy + (int) ((tableRadius - 40) * Math.sin(angle));

            g2d.setPaint(new GradientPaint(plateX - 12, plateY - 12, new Color(240, 240, 240), plateX + 12, plateY + 12, new Color(180, 180, 180)));
            g2d.fillOval(plateX - 14, plateY - 14, 28, 28);
            g2d.setColor(new Color(220, 220, 220));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(plateX - 10, plateY - 10, 20, 20);

            int px = cx + (int) ((tableRadius + 65) * Math.cos(angle));
            int py = cy + (int) ((tableRadius + 65) * Math.sin(angle));

            Color pColor;
            String stateStr = "";
            if (simulationPaused) {
                pColor = new Color(108, 117, 125);
                stateStr = "PAUSED";
            } else {
                switch (p.state) {
                    case THINKING:
                        pColor = new Color(52, 152, 219);
                        stateStr = "Thinking";
                        break;
                    case EATING:
                        pColor = new Color(46, 204, 113);
                        stateStr = "Eating";
                        break;
                    default:
                        pColor = new Color(243, 156, 18);
                        stateStr = "Waiting";
                        break;
                }
            }

            g2d.setColor(new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 40));
            g2d.fillOval(px - 32, py - 32, 64, 64);

            g2d.setPaint(new GradientPaint(px - 25, py - 25, pColor.brighter(), px + 25, py + 25, pColor.darker()));
            g2d.fillOval(px - 25, py - 25, 50, 50);
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(px - 25, py - 25, 50, 50);

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            g2d.drawString("P " + i, px - 11, py - 35);

            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2d.setColor(new Color(200, 200, 200));
            g2d.drawString(stateStr, px - (g2d.getFontMetrics().stringWidth(stateStr)/2), py + 42);

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2d.setColor(new Color(46, 204, 113));
            String eatsText = "Meals: " + p.eatCount;
            g2d.drawString(eatsText, px - (g2d.getFontMetrics().stringWidth(eatsText)/2), py + 58);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simulation: Dining Philosophers Dashboard");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setSize(850, 850); frame.setLocationRelativeTo(null); frame.setLayout(new BorderLayout());

            DiningPhilosophersSwing panel = new DiningPhilosophersSwing();
            frame.add(panel, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel();
            controlPanel.setBackground(new Color(33, 33, 38));
            controlPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 55)));
            controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));

            JButton addBtn = new JButton("➕ Add Philosopher");
            addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            addBtn.setFocusPainted(false);
            addBtn.addActionListener(e -> {
                if (panel.numPhilosophers < panel.MAX_PHILOSOPHERS) {
                    panel.initSystem(panel.numPhilosophers + 1);
                    if(panel.numPhilosophers == panel.MAX_PHILOSOPHERS) addBtn.setEnabled(false);
                }
            });
            controlPanel.add(addBtn);

            JButton pauseBtn = new JButton("⏸ Pause Simulation");
            pauseBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            pauseBtn.setFocusPainted(false);
            pauseBtn.addActionListener(e -> {
                panel.simulationPaused = !panel.simulationPaused;
                if (panel.simulationPaused) {
                    pauseBtn.setText("▶ Resume Simulation");
                } else {
                    pauseBtn.setText("⏸ Pause Simulation");
                }
            });
            controlPanel.add(pauseBtn);

            frame.add(controlPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }
}
