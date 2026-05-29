import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophersSwing extends JPanel {

    // --- Enums & Models ---
    enum State {
        THINKING, WAITING_FOR_FORK_1, WAITING_FOR_FORK_2, EATING
    }

    static class Fork {
        int id;
        ReentrantLock lock = new ReentrantLock();
        volatile int ownerId = -1; // -1 means free

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
        volatile boolean paused = false;
        Random random = new Random();

        public Philosopher(int id, Fork leftFork, Fork rightFork) {
            this.id = id;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
        }

        public void stopThread() {
            running = false;
        }

        public void togglePause() {
            paused = !paused;
        }

        @Override
        public void run() {
            try {
                while (running) {
                    if (paused) {
                        Thread.sleep(100);
                        continue;
                    }

                    // 1. Thinking (up to 5 seconds)
                    state = State.THINKING;
                    Thread.sleep(random.nextInt(5000));

                    if (paused || !running) continue;

                    // Asymmetric Strategy to prevent Deadlock:
                    // Evens take left first, Odds take right first.
                    Fork firstFork = (id % 2 == 0) ? leftFork : rightFork;
                    Fork secondFork = (id % 2 == 0) ? rightFork : leftFork;

                    // 2. Try grabbing Fork 1
                    state = State.WAITING_FOR_FORK_1;
                    while (!firstFork.lock.tryLock()) {
                        Thread.sleep(100); // Poll every 100ms
                        if (!running) return;
                    }
                    firstFork.ownerId = this.id;

                    // 3. Wait random time up to 1 second
                    Thread.sleep(random.nextInt(1000));

                    if (!running) {
                        firstFork.ownerId = -1;
                        firstFork.lock.unlock();
                        return;
                    }

                    // 4. Try grabbing Fork 2
                    state = State.WAITING_FOR_FORK_2;
                    while (!secondFork.lock.tryLock()) {
                        Thread.sleep(100); // Poll every 100ms
                        if (!running) return;
                    }
                    secondFork.ownerId = this.id;

                    // 5. Eating (up to 1 second)
                    state = State.EATING;
                    eatCount++;
                    Thread.sleep(random.nextInt(1000));

                    // 6. Release both forks
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

    // --- System State ---
    int numPhilosophers = 5;
    final int MAX_PHILOSOPHERS = 7;
    List<Philosopher> philosophers = new ArrayList<>();
    List<Fork> forks = new ArrayList<>();
    List<Thread> threads = new ArrayList<>();

    // UI Controls
    JComboBox<String> philosopherSelector;

    public DiningPhilosophersSwing() {
        initSystem(numPhilosophers);

        // Timer to repaint GUI at ~30 FPS
        Timer timer = new Timer(33, e -> repaint());
        timer.start();
    }

    private void initSystem(int n) {
        // Stop existing threads if any
        for (Philosopher p : philosophers) {
            p.stopThread();
        }
        philosophers.clear();
        forks.clear();
        threads.clear();

        this.numPhilosophers = n;

        // Create Forks
        for (int i = 0; i < n; i++) {
            forks.add(new Fork(i));
        }

        // Create Philosophers
        for (int i = 0; i < n; i++) {
            Fork left = forks.get(i);
            Fork right = forks.get((i + 1) % n);
            Philosopher p = new Philosopher(i, left, right);
            philosophers.add(p);

            Thread t = new Thread(p);
            threads.add(t);
            t.start();
        }
    }

    // --- Drawing Logic ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2;
        int tableRadius = Math.min(width, height) / 4;

        // Draw Table
        g2d.setColor(new Color(139, 69, 19)); // Brown
        g2d.fillOval(cx - tableRadius, cy - tableRadius, tableRadius * 2, tableRadius * 2);

        // Calculate positions
        double angleStep = 2 * Math.PI / numPhilosophers;

        // Draw Forks
        for (int i = 0; i < numPhilosophers; i++) {
            Fork fork = forks.get(i);

            // Fork default position is between philosophers
            double forkAngle = i * angleStep + (angleStep / 2.0);
            int fx = cx + (int) ((tableRadius - 20) * Math.cos(forkAngle));
            int fy = cy + (int) ((tableRadius - 20) * Math.sin(forkAngle));

            // If fork is held, move it closer to the owner
            if (fork.ownerId != -1) {
                g2d.setColor(Color.YELLOW); // Held fork color
                double ownerAngle = fork.ownerId * angleStep;
                fx = cx + (int) ((tableRadius - 40) * Math.cos(ownerAngle));
                fy = cy + (int) ((tableRadius - 40) * Math.sin(ownerAngle));
            } else {
                g2d.setColor(Color.LIGHT_GRAY); // Free fork color
            }
            g2d.fillOval(fx - 5, fy - 5, 10, 10);
        }

        // Draw Philosophers & Plates
        for (int i = 0; i < numPhilosophers; i++) {
            Philosopher p = philosophers.get(i);
            double angle = i * angleStep;
            int px = cx + (int) ((tableRadius + 50) * Math.cos(angle));
            int py = cy + (int) ((tableRadius + 50) * Math.sin(angle));

            // Draw Plate
            int plateX = cx + (int) ((tableRadius - 20) * Math.cos(angle));
            int plateY = cy + (int) ((tableRadius - 20) * Math.sin(angle));
            g2d.setColor(Color.WHITE);
            g2d.fillOval(plateX - 15, plateY - 15, 30, 30);

            // Determine Philosopher Color
            if (p.state == State.THINKING) {
                g2d.setColor(Color.BLUE);
            } else if (p.state == State.EATING) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.ORANGE); // Waiting
            }
            if(p.paused) {
                g2d.setColor(Color.GRAY);
            }

            // Draw Philosopher
            g2d.fillOval(px - 25, py - 25, 50, 50);

            // Draw Texts
            g2d.setColor(Color.BLACK);
            g2d.drawString("P" + i + (p.paused ? " (PAUSED)" : ""), px - 10, py - 30);
            g2d.drawString("Eats: " + p.eatCount, px - 20, py + 40);
            g2d.drawString(p.state.toString(), px - 40, py + 55);
        }
    }

    // --- Main Method & UI Setup ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dining Philosophers");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 800);
            frame.setLayout(new BorderLayout());

            DiningPhilosophersSwing panel = new DiningPhilosophersSwing();
            frame.add(panel, BorderLayout.CENTER);

            // Control Panel
            JPanel controlPanel = new JPanel();

            // Add Philosopher Button
            JButton addBtn = new JButton("Add Philosopher");
            addBtn.addActionListener(e -> {
                if (panel.numPhilosophers < panel.MAX_PHILOSOPHERS) {
                    panel.initSystem(panel.numPhilosophers + 1);
                    panel.updateComboBox();
                    if(panel.numPhilosophers == panel.MAX_PHILOSOPHERS) {
                        addBtn.setEnabled(false);
                    }
                }
            });
            controlPanel.add(addBtn);

            // Pause Logic
            panel.philosopherSelector = new JComboBox<>();
            panel.updateComboBox();
            controlPanel.add(panel.philosopherSelector);

            JButton pauseBtn = new JButton("Toggle Pause");
            pauseBtn.addActionListener(e -> {
                int selected = panel.philosopherSelector.getSelectedIndex();
                if(selected != -1 && selected < panel.philosophers.size()) {
                    panel.philosophers.get(selected).togglePause();
                }
            });
            controlPanel.add(pauseBtn);

            frame.add(controlPanel, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }

    private void updateComboBox() {
        philosopherSelector.removeAllItems();
        for(int i = 0; i < numPhilosophers; i++) {
            philosopherSelector.addItem("Philosopher " + i);
        }
    }
}