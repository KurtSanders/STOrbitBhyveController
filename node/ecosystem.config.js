module.exports = {
  apps : [{
    name: 'Orbit',
    script: 'node',
    args: '-r dotenv/config app.js dotenv_config_path=/home/pi/bhyve/.env',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    exp_backoff_restart_delay: 10000,
    restart_delay: 10000,
    min_uptime: 5000,
    kill_timeout: 1000,
    wait_ready: true,
    max_restarts: 2
  }],
};

